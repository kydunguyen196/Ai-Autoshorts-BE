package com.autoshorts.ai.service;

import com.autoshorts.ai.config.AppProperties;
import com.autoshorts.ai.entity.VideoJob;
import com.autoshorts.ai.entity.WebhookDelivery;
import com.autoshorts.ai.entity.WebhookDeliveryStatus;
import com.autoshorts.ai.entity.WebhookEventType;
import com.autoshorts.ai.repository.WebhookDeliveryRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class WebhookDeliveryService {

    private static final Logger log = LoggerFactory.getLogger(WebhookDeliveryService.class);

    private final WebhookDeliveryRepository webhookDeliveryRepository;
    private final ObjectMapper objectMapper;
    private final AppProperties appProperties;
    private final WebClient webClient;
    private final TransactionTemplate transactionTemplate;

    public WebhookDeliveryService(
        WebhookDeliveryRepository webhookDeliveryRepository,
        ObjectMapper objectMapper,
        AppProperties appProperties,
        WebClient.Builder webClientBuilder,
        PlatformTransactionManager transactionManager
    ) {
        this.webhookDeliveryRepository = webhookDeliveryRepository;
        this.objectMapper = objectMapper;
        this.appProperties = appProperties;
        this.webClient = webClientBuilder.build();
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    @Transactional
    public void enqueueJobEvent(WebhookEventType eventType, VideoJob job) {
        enqueueJobEvent(eventType, job, Map.of());
    }

    @Transactional
    public void enqueueJobEvent(WebhookEventType eventType, VideoJob job, Map<String, Object> metadata) {
        if (!isWebhookEnabled()) {
            return;
        }

        WebhookDelivery delivery = new WebhookDelivery();
        delivery.setId(UUID.randomUUID());
        delivery.setJobId(job.getId());
        delivery.setEventType(eventType.getEventName());
        delivery.setEndpointUrl(appProperties.getWebhook().getEndpointUrl().trim());
        delivery.setPayloadJson(buildPayload(eventType, job, metadata));
        delivery.setStatus(WebhookDeliveryStatus.PENDING);
        delivery.setAttemptCount(0);
        delivery.setMaxAttempts(appProperties.getWebhook().getMaxAttempts());
        delivery.setNextAttemptAt(Instant.now());

        webhookDeliveryRepository.save(delivery);

        log.info(
            "event=webhook_delivery_enqueued deliveryId={} jobId={} eventType={} endpoint={}",
            delivery.getId(),
            job.getId(),
            eventType.getEventName(),
            delivery.getEndpointUrl()
        );
    }

    public int dispatchDueDeliveries() {
        if (!isWebhookEnabled()) {
            return 0;
        }

        Integer processed;
        try {
            processed = transactionTemplate.execute(status -> {
                Instant now = Instant.now();
                int batchSize = Math.max(1, appProperties.getWebhook().getDispatchBatchSize());
                List<WebhookDelivery> due = webhookDeliveryRepository.findDueDeliveriesForUpdate(now, batchSize);
                if (due.isEmpty()) {
                    return 0;
                }

                int done = 0;
                for (WebhookDelivery delivery : due) {
                    processDelivery(delivery, now);
                    done++;
                }
                return done;
            });
        } catch (Exception ex) {
            log.warn("event=webhook_dispatch_batch_skipped message={}", ex.getMessage());
            return 0;
        }

        if (processed == null) {
            return 0;
        }
        return processed;
    }

    private void processDelivery(WebhookDelivery delivery, Instant now) {
        int currentAttempt = (delivery.getAttemptCount() == null ? 0 : delivery.getAttemptCount()) + 1;
        delivery.setAttemptCount(currentAttempt);
        delivery.setLastAttemptAt(now);

        try {
            WebhookHttpResult result = webClient.post()
                .uri(delivery.getEndpointUrl())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(delivery.getPayloadJson())
                .exchangeToMono(response -> response.bodyToMono(String.class)
                    .defaultIfEmpty("")
                    .map(body -> new WebhookHttpResult(response.statusCode().value(), body)))
                .block(Duration.ofSeconds(appProperties.getWebhook().getRequestTimeoutSeconds()));

            if (result == null) {
                markRetryableFailure(delivery, "empty_response", null, now);
            } else if (result.statusCode() >= 200 && result.statusCode() < 300) {
                delivery.setStatus(WebhookDeliveryStatus.DELIVERED);
                delivery.setLastResponseStatus(result.statusCode());
                delivery.setLastErrorMessage(null);
                delivery.setNextAttemptAt(now);
                log.info(
                    "event=webhook_delivery_success deliveryId={} jobId={} eventType={} statusCode={} attempt={}",
                    delivery.getId(),
                    delivery.getJobId(),
                    delivery.getEventType(),
                    result.statusCode(),
                    currentAttempt
                );
            } else {
                String errorMessage = "status=%s body=%s".formatted(
                    result.statusCode(),
                    truncate(result.body(), 500)
                );
                markRetryableFailure(delivery, errorMessage, result.statusCode(), now);
            }
        } catch (Exception ex) {
            markRetryableFailure(delivery, truncate(ex.getMessage(), 500), null, now);
        }

        webhookDeliveryRepository.save(delivery);
    }

    private void markRetryableFailure(WebhookDelivery delivery, String errorMessage, Integer statusCode, Instant now) {
        int attempt = delivery.getAttemptCount() == null ? 0 : delivery.getAttemptCount();
        int maxAttempts = delivery.getMaxAttempts() == null
            ? appProperties.getWebhook().getMaxAttempts()
            : delivery.getMaxAttempts();

        delivery.setLastResponseStatus(statusCode);
        delivery.setLastErrorMessage(errorMessage);

        if (attempt >= maxAttempts) {
            delivery.setStatus(WebhookDeliveryStatus.FAILED);
            delivery.setNextAttemptAt(now);
            log.warn(
                "event=webhook_delivery_failed_final deliveryId={} jobId={} eventType={} attempt={} maxAttempts={} error={}",
                delivery.getId(),
                delivery.getJobId(),
                delivery.getEventType(),
                attempt,
                maxAttempts,
                errorMessage
            );
            return;
        }

        long delayMs = computeRetryDelayMs(attempt);
        delivery.setStatus(WebhookDeliveryStatus.PENDING);
        delivery.setNextAttemptAt(now.plusMillis(delayMs));
        log.warn(
            "event=webhook_delivery_failed_retry deliveryId={} jobId={} eventType={} attempt={} nextAttemptAt={} error={}",
            delivery.getId(),
            delivery.getJobId(),
            delivery.getEventType(),
            attempt,
            delivery.getNextAttemptAt(),
            errorMessage
        );
    }

    private long computeRetryDelayMs(int attempt) {
        long initial = appProperties.getWebhook().getRetryInitialDelayMs();
        long max = appProperties.getWebhook().getRetryMaxDelayMs();
        double multiplier = appProperties.getWebhook().getRetryMultiplier();

        if (attempt <= 1) {
            return Math.min(initial, max);
        }

        double value = initial * Math.pow(multiplier, Math.max(0, attempt - 1));
        long backoff = (long) value;
        return Math.max(initial, Math.min(backoff, max));
    }

    private String buildPayload(WebhookEventType eventType, VideoJob job, Map<String, Object> metadata) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("version", "2026-04-character-story-ads-v1");
        payload.put("event", eventType.getEventName());
        payload.put("occurredAt", Instant.now());
        payload.put("ownerUserId", job.getUserId());
        payload.put("channelId", job.getChannelId());

        Map<String, Object> jobPayload = new LinkedHashMap<>();
        jobPayload.put("jobId", job.getId());
        jobPayload.put("userId", job.getUserId());
        jobPayload.put("channelId", job.getChannelId());
        jobPayload.put("status", job.getStatus());
        jobPayload.put("currentStep", job.getCurrentStep());
        jobPayload.put("topic", job.getTopic());
        jobPayload.put("style", job.getStyle());
        jobPayload.put("finalVideoUrl", job.getFinalVideoUrl());
        jobPayload.put("audioUrl", job.getAudioUrl());
        jobPayload.put("audioGenerationMode", job.getAudioGenerationMode());
        jobPayload.put("audioProvider", job.getAudioProvider());
        jobPayload.put("errorMessage", job.getErrorMessage());
        jobPayload.put("publishStatus", job.getPublishStatus());
        jobPayload.put("publishPlatform", job.getPublishPlatform());
        jobPayload.put("publishProvider", job.getPublishProvider());
        jobPayload.put("publishExternalId", job.getPublishExternalId());
        jobPayload.put("publishTargetAccountId", job.getPublishTargetAccountId());
        jobPayload.put("publishAttemptCount", job.getPublishAttemptCount());
        jobPayload.put("publishFailureReason", job.getPublishFailureReason());
        jobPayload.put("publishFailureDetails", job.getPublishFailureDetails());
        jobPayload.put("publishRequestPayloadJson", job.getPublishRequestPayloadJson());
        jobPayload.put("publishResponsePayloadJson", job.getPublishResponsePayloadJson());
        jobPayload.put("publishRequestedAt", job.getPublishRequestedAt());
        jobPayload.put("publishStartedAt", job.getPublishStartedAt());
        jobPayload.put("publishedAt", job.getPublishedAt());
        jobPayload.put("reviewStatus", job.getReviewStatus());
        jobPayload.put("reviewedAt", job.getReviewedAt());
        jobPayload.put("reviewedBy", job.getReviewedBy());
        jobPayload.put("rejectionReason", job.getRejectionReason());
        jobPayload.put("selectedForPublish", job.getSelectedForPublish());
        jobPayload.put("generationBatchId", job.getGenerationBatchId());
        jobPayload.put("generationGroupId", job.getGenerationGroupId());
        jobPayload.put("variantIndex", job.getVariantIndex());
        jobPayload.put("variantCount", job.getVariantCount());
        jobPayload.put("rankingScore", job.getRankingScore());
        jobPayload.put("topCandidate", job.getTopCandidate());
        jobPayload.put("topCandidateRank", job.getTopCandidateRank());
        jobPayload.put("characterProfileId", job.getCharacterProfileId());
        jobPayload.put("characterCampaignId", job.getCharacterCampaignId());
        jobPayload.put("storyAngle", job.getStoryAngle());
        jobPayload.put("productPlacementMode", job.getProductPlacementMode());
        jobPayload.put("adDisclosureMode", job.getAdDisclosureMode());
        jobPayload.put("sceneCountTarget", job.getSceneCountTarget());
        jobPayload.put("characterConsistencyMode", job.getCharacterConsistencyMode());
        jobPayload.put("createdAt", job.getCreatedAt());
        jobPayload.put("updatedAt", job.getUpdatedAt());

        payload.put("job", jobPayload);
        if (metadata != null && !metadata.isEmpty()) {
            payload.put("metadata", metadata);
        }

        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to serialize webhook payload", ex);
        }
    }

    private boolean isWebhookEnabled() {
        return appProperties.getWebhook().isEnabled()
            && StringUtils.hasText(appProperties.getWebhook().getEndpointUrl());
    }

    private String truncate(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        if (value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }

    private record WebhookHttpResult(int statusCode, String body) {
    }
}
