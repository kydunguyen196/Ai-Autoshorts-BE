package com.autoshorts.ai.service;

import com.autoshorts.ai.cache.VideoJobStateCache;
import com.autoshorts.ai.config.AppProperties;
import com.autoshorts.ai.dto.VideoJobResponse;
import com.autoshorts.ai.dto.VideoPublishStatusResponse;
import com.autoshorts.ai.entity.JobStatus;
import com.autoshorts.ai.entity.PublishStatus;
import com.autoshorts.ai.entity.ReviewStatus;
import com.autoshorts.ai.entity.SocialPlatform;
import com.autoshorts.ai.entity.TikTokAccountConnection;
import com.autoshorts.ai.entity.VideoJob;
import com.autoshorts.ai.entity.WebhookEventType;
import com.autoshorts.ai.exception.ExternalServiceException;
import com.autoshorts.ai.exception.InvalidJobStateException;
import com.autoshorts.ai.exception.ResourceNotFoundException;
import com.autoshorts.ai.publish.PublishRequest;
import com.autoshorts.ai.publish.PublishResult;
import com.autoshorts.ai.publish.VideoPublisher;
import com.autoshorts.ai.repository.VideoJobRepository;
import com.autoshorts.ai.util.VideoJobMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class VideoPublishService {

    private static final Logger log = LoggerFactory.getLogger(VideoPublishService.class);

    private final VideoJobRepository videoJobRepository;
    private final VideoJobStateCache videoJobStateCache;
    private final AppProperties appProperties;
    private final List<VideoPublisher> publishers;
    private final WebhookDeliveryService webhookDeliveryService;
    private final TikTokConnectionService tikTokConnectionService;
    private final SocialConnectionService socialConnectionService;
    private final TransactionTemplate transactionTemplate;

    public VideoPublishService(
        VideoJobRepository videoJobRepository,
        VideoJobStateCache videoJobStateCache,
        AppProperties appProperties,
        List<VideoPublisher> publishers,
        WebhookDeliveryService webhookDeliveryService,
        TikTokConnectionService tikTokConnectionService,
        SocialConnectionService socialConnectionService,
        PlatformTransactionManager transactionManager
    ) {
        this.videoJobRepository = videoJobRepository;
        this.videoJobStateCache = videoJobStateCache;
        this.appProperties = appProperties;
        this.publishers = publishers;
        this.webhookDeliveryService = webhookDeliveryService;
        this.tikTokConnectionService = tikTokConnectionService;
        this.socialConnectionService = socialConnectionService;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    public VideoJobResponse publish(UUID jobId, UUID userId, String requestedPlatform) {
        if (!appProperties.getPublish().isEnabled()) {
            throw new InvalidJobStateException("Publish pipeline is disabled");
        }

        PublishAttemptContext context = beginPublishAttempt(jobId, userId, requestedPlatform);
        if (!context.attemptStarted()) {
            return VideoJobMapper.toResponse(context.job());
        }

        emitWebhookSafely(WebhookEventType.PUBLISH_REQUESTED, context.job(), Map.of(
            "provider", context.providerKey(),
            "platform", context.platform()
        ));

        PublishResult result;
        try {
            result = resolvePublisher(context.providerKey()).publish(new PublishRequest(
                context.job().getId(),
                context.job().getUserId(),
                context.job().getChannelId(),
                context.job().getTopic(),
                context.platform(),
                context.job().getFinalVideoUrl(),
                context.job().getCaptionText(),
                context.targetAccountId()
            ));
        } catch (Exception ex) {
            VideoJob failed = markPublishFailed(jobId, userId, context.providerKey(), context.targetAccountId(), ex);
            emitWebhookSafely(WebhookEventType.PUBLISH_FAILED, failed, Map.of("provider", context.providerKey()));
            emitWebhookSafely(WebhookEventType.JOB_PUBLISH_FAILED, failed, Map.of("provider", context.providerKey()));
            throw new ExternalServiceException("Video publish failed: " + truncate(ex.getMessage(), 300));
        }

        if (result.pendingAsync()) {
            // Provider accepted the post but completion is asynchronous (e.g. TikTok processing).
            // Keep the job PUBLISHING; TikTokPublishStatusService will finalize it after polling.
            VideoJob submitted = markPublishSubmitted(jobId, userId, context, result);
            log.info(
                "event=publish_attempt_submitted_async jobId={} provider={} externalId={}",
                submitted.getId(),
                submitted.getPublishProvider(),
                submitted.getPublishExternalId()
            );
            return VideoJobMapper.toResponse(submitted);
        }

        VideoJob published = markPublished(jobId, userId, context, result);
        emitWebhookSafely(WebhookEventType.PUBLISH_SUCCEEDED, published, Map.of("provider", context.providerKey()));
        emitWebhookSafely(WebhookEventType.JOB_PUBLISHED, published, Map.of("provider", context.providerKey()));
        return VideoJobMapper.toResponse(published);
    }

    @Transactional(readOnly = true)
    public VideoPublishStatusResponse getPublishStatus(UUID jobId, UUID userId) {
        VideoJob job = videoJobRepository.findByIdAndUserId(jobId, userId)
            .orElseThrow(() -> new ResourceNotFoundException("Job not found: " + jobId));

        VideoPublishStatusResponse response = VideoJobMapper.toPublishStatusResponse(job);
        PublishReadiness readiness = evaluateReadiness(job);
        response.setPublishable(readiness.publishable());
        response.setPublishReadinessReason(readiness.reason());
        response.setTiktokConnectionStatus(readiness.tiktokConnectionStatus());
        return response;
    }

    protected PublishAttemptContext beginPublishAttempt(UUID jobId, UUID userId, String requestedPlatform) {
        return transactionTemplate.execute(status -> {
            VideoJob job = videoJobRepository.findByIdAndUserIdForUpdate(jobId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Job not found: " + jobId));

            if (job.getStatus() != JobStatus.COMPLETED) {
                throw new InvalidJobStateException("Only COMPLETED jobs can be published. Current state: " + job.getStatus());
            }
            if (!StringUtils.hasText(job.getFinalVideoUrl())) {
                throw new InvalidJobStateException("Cannot publish without a generated final video URL");
            }
            if (job.getReviewStatus() != ReviewStatus.APPROVED) {
                throw new InvalidJobStateException("Only APPROVED jobs can be published");
            }
            if (requiresPreferredSelection(job) && !Boolean.TRUE.equals(job.getSelectedForPublish())) {
                throw new InvalidJobStateException("Select a preferred variant for publish before publishing this group");
            }

            if (job.getPublishStatus() == PublishStatus.PUBLISHED) {
                return new PublishAttemptContext(job, false, job.getPublishPlatform(), job.getPublishProvider(), job.getPublishTargetAccountId());
            }
            if (job.getPublishStatus() == PublishStatus.PUBLISHING) {
                throw new InvalidJobStateException("Job publish is already in progress");
            }

            String platform = resolvePlatform(requestedPlatform, job.getPublishPlatform());
            String providerKey = resolveProviderKey(platform);
            String targetAccountId = resolveTargetAccountId(job, userId, platform);
            Instant now = Instant.now();

            job.setPublishPlatform(platform);
            job.setPublishStatus(PublishStatus.PUBLISHING);
            job.setPublishProvider(providerKey);
            job.setPublishTargetAccountId(targetAccountId);
            job.setPublishRequestPayloadJson(buildPublishRequestSnapshot(job, platform, providerKey, targetAccountId));
            job.setPublishResponsePayloadJson(null);
            job.setPublishRequestedAt(now);
            job.setPublishStartedAt(now);
            if (job.getPublishReadyAt() == null) {
                job.setPublishReadyAt(now);
            }
            job.setPublishFailureReason(null);
            job.setPublishFailureDetails(null);
            job.setPublishLastErrorAt(null);
            job.setPublishAttemptCount((job.getPublishAttemptCount() == null ? 0 : job.getPublishAttemptCount()) + 1);

            VideoJob saved = saveAndCache(job);

            log.info(
                "event=publish_attempt_started jobId={} userId={} platform={} attempt={} provider={} targetAccountId={}",
                saved.getId(),
                saved.getUserId(),
                saved.getPublishPlatform(),
                saved.getPublishAttemptCount(),
                providerKey,
                targetAccountId
            );

            return new PublishAttemptContext(saved, true, platform, providerKey, targetAccountId);
        });
    }

    protected VideoJob markPublished(UUID jobId, UUID userId, PublishAttemptContext context, PublishResult result) {
        return transactionTemplate.execute(status -> {
            VideoJob job = videoJobRepository.findByIdAndUserIdForUpdate(jobId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Job not found: " + jobId));

            Instant now = Instant.now();
            job.setPublishStatus(PublishStatus.PUBLISHED);
            job.setPublishPlatform(context.platform());
            job.setPublishedAt(now);
            job.setPublishProvider(result.provider());
            job.setPublishExternalId(result.externalId());
            job.setPublishTargetAccountId(firstNonBlank(result.targetAccountId(), context.targetAccountId()));
            job.setPublishRequestPayloadJson(firstNonBlank(result.requestPayloadJson(), job.getPublishRequestPayloadJson()));
            job.setPublishResponsePayloadJson(firstNonBlank(result.responsePayloadJson(), job.getPublishResponsePayloadJson()));
            job.setPublishFailureReason(null);
            job.setPublishFailureDetails(null);
            job.setPublishLastErrorAt(null);
            job.setPublishLastStatusCheckAt(now);

            VideoJob saved = saveAndCache(job);
            log.info(
                "event=publish_attempt_succeeded jobId={} platform={} provider={} externalId={} attempt={}",
                saved.getId(),
                saved.getPublishPlatform(),
                saved.getPublishProvider(),
                saved.getPublishExternalId(),
                saved.getPublishAttemptCount()
            );
            return saved;
        });
    }

    /** Record an accepted-but-pending async post; the job stays PUBLISHING until reconciled. */
    protected VideoJob markPublishSubmitted(UUID jobId, UUID userId, PublishAttemptContext context, PublishResult result) {
        return transactionTemplate.execute(status -> {
            VideoJob job = videoJobRepository.findByIdAndUserIdForUpdate(jobId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Job not found: " + jobId));

            job.setPublishStatus(PublishStatus.PUBLISHING);
            job.setPublishPlatform(context.platform());
            job.setPublishProvider(result.provider());
            job.setPublishExternalId(result.externalId());
            job.setPublishTargetAccountId(firstNonBlank(result.targetAccountId(), context.targetAccountId()));
            job.setPublishRequestPayloadJson(firstNonBlank(result.requestPayloadJson(), job.getPublishRequestPayloadJson()));
            job.setPublishResponsePayloadJson(firstNonBlank(result.responsePayloadJson(), job.getPublishResponsePayloadJson()));
            job.setPublishFailureReason(null);
            job.setPublishFailureDetails(null);
            job.setPublishLastErrorAt(null);
            // Leave publishLastStatusCheckAt null so the reconciler picks it up on the next cycle.
            job.setPublishLastStatusCheckAt(null);
            return saveAndCache(job);
        });
    }

    /**
     * Finalize an async post as PUBLISHED. Idempotent: no-op if the job is no longer PUBLISHING.
     * Called by the status reconciler (system context — no per-user ownership check).
     */
    public VideoJob completeAsyncPublish(UUID jobId, String externalId, String responsePayloadJson) {
        AsyncTransition transition = transactionTemplate.execute(status -> {
            VideoJob job = videoJobRepository.findByIdForUpdate(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("Job not found: " + jobId));
            if (job.getPublishStatus() != PublishStatus.PUBLISHING) {
                return new AsyncTransition(job, false);
            }
            Instant now = Instant.now();
            job.setPublishStatus(PublishStatus.PUBLISHED);
            job.setPublishedAt(now);
            if (StringUtils.hasText(externalId)) {
                job.setPublishExternalId(externalId);
            }
            if (StringUtils.hasText(responsePayloadJson)) {
                job.setPublishResponsePayloadJson(responsePayloadJson);
            }
            job.setPublishFailureReason(null);
            job.setPublishFailureDetails(null);
            job.setPublishLastErrorAt(null);
            job.setPublishLastStatusCheckAt(now);
            return new AsyncTransition(saveAndCache(job), true);
        });

        if (transition.changed()) {
            log.info(
                "event=publish_async_completed jobId={} provider={} externalId={}",
                transition.job().getId(),
                transition.job().getPublishProvider(),
                transition.job().getPublishExternalId()
            );
            emitWebhookSafely(WebhookEventType.PUBLISH_SUCCEEDED, transition.job(), Map.of("provider", safe(transition.job().getPublishProvider())));
            emitWebhookSafely(WebhookEventType.JOB_PUBLISHED, transition.job(), Map.of("provider", safe(transition.job().getPublishProvider())));
        }
        return transition.job();
    }

    /**
     * Finalize an async post as PUBLISH_FAILED. Idempotent: no-op if the job is no longer PUBLISHING.
     */
    public VideoJob failAsyncPublish(UUID jobId, String reason, String details) {
        AsyncTransition transition = transactionTemplate.execute(status -> {
            VideoJob job = videoJobRepository.findByIdForUpdate(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("Job not found: " + jobId));
            if (job.getPublishStatus() != PublishStatus.PUBLISHING) {
                return new AsyncTransition(job, false);
            }
            Instant now = Instant.now();
            job.setPublishStatus(PublishStatus.PUBLISH_FAILED);
            job.setPublishFailureReason(truncate(reason, 500));
            job.setPublishFailureDetails(truncate(details, 4000));
            job.setPublishLastErrorAt(now);
            job.setPublishLastStatusCheckAt(now);
            return new AsyncTransition(saveAndCache(job), true);
        });

        if (transition.changed()) {
            log.warn(
                "event=publish_async_failed jobId={} provider={} reason={}",
                transition.job().getId(),
                transition.job().getPublishProvider(),
                transition.job().getPublishFailureReason()
            );
            emitWebhookSafely(WebhookEventType.PUBLISH_FAILED, transition.job(), Map.of("provider", safe(transition.job().getPublishProvider())));
            emitWebhookSafely(WebhookEventType.JOB_PUBLISH_FAILED, transition.job(), Map.of("provider", safe(transition.job().getPublishProvider())));
        }
        return transition.job();
    }

    /** Record that a status check happened without a state change (post still processing). */
    public void touchPublishStatusCheck(UUID jobId) {
        transactionTemplate.executeWithoutResult(status -> {
            videoJobRepository.findByIdForUpdate(jobId).ifPresent(job -> {
                job.setPublishLastStatusCheckAt(Instant.now());
                saveAndCache(job);
            });
        });
    }

    protected VideoJob markPublishFailed(
        UUID jobId,
        UUID userId,
        String providerKey,
        String targetAccountId,
        Exception cause
    ) {
        return transactionTemplate.execute(status -> {
            VideoJob job = videoJobRepository.findByIdAndUserIdForUpdate(jobId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Job not found: " + jobId));

            Instant now = Instant.now();
            job.setPublishStatus(PublishStatus.PUBLISH_FAILED);
            job.setPublishProvider(providerKey);
            job.setPublishTargetAccountId(targetAccountId);
            job.setPublishFailureReason(truncate(cause.getMessage(), 500));
            job.setPublishFailureDetails(buildFailureDetails(cause));
            job.setPublishLastErrorAt(now);
            job.setPublishLastStatusCheckAt(now);

            VideoJob saved = saveAndCache(job);
            log.warn(
                "event=publish_attempt_failed jobId={} platform={} attempt={} provider={} error={}",
                saved.getId(),
                saved.getPublishPlatform(),
                saved.getPublishAttemptCount(),
                saved.getPublishProvider(),
                saved.getPublishFailureReason()
            );
            return saved;
        });
    }

    private VideoJob saveAndCache(VideoJob job) {
        VideoJob saved = videoJobRepository.save(job);
        videoJobStateCache.put(VideoJobMapper.toResponse(saved));
        return saved;
    }

    private VideoPublisher resolvePublisher(String providerKey) {
        return publishers.stream()
            .filter(publisher -> publisher.providerKey().equalsIgnoreCase(providerKey))
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("No publish provider configured for key: " + providerKey));
    }

    private String resolveProviderKey(String platform) {
        if (isTikTokPlatform(platform)) {
            return "tiktok";
        }
        return SocialPlatform.fromKey(platform)
            .map(SocialPlatform::key)
            .orElse(appProperties.getPublish().getProvider());
    }

    private String resolvePlatform(String requestedPlatform, String existingPlatform) {
        if (StringUtils.hasText(requestedPlatform)) {
            return requestedPlatform.trim();
        }
        if (StringUtils.hasText(existingPlatform)) {
            return existingPlatform;
        }
        return appProperties.getPublish().getDefaultPlatform();
    }

    private String resolveTargetAccountId(VideoJob job, UUID userId, String platform) {
        if (isTikTokPlatform(platform)) {
            TikTokAccountConnection connection = tikTokConnectionService.findActiveConnection(userId, job.getChannelId())
                .orElseThrow(() -> new InvalidJobStateException("Active TikTok account connection is required before publishing"));
            return connection.getPlatformAccountId();
        }

        Optional<SocialPlatform> socialPlatform = SocialPlatform.fromKey(platform);
        if (socialPlatform.isPresent()) {
            SocialPlatform sp = socialPlatform.get();
            return socialConnectionService.findActiveConnection(userId, job.getChannelId(), sp)
                .orElseThrow(() -> new InvalidJobStateException(
                    "Active " + sp.key() + " account connection is required before publishing"))
                .getPlatformAccountId();
        }
        return null;
    }

    private PublishReadiness evaluateReadiness(VideoJob job) {
        String platform = resolvePlatform(null, job.getPublishPlatform());
        Optional<SocialPlatform> socialPlatform = SocialPlatform.fromKey(platform);
        String connectionStatus;
        if (isTikTokPlatform(platform)) {
            connectionStatus = tikTokConnectionService.describeConnectionState(job.getUserId(), job.getChannelId());
        } else if (socialPlatform.isPresent()) {
            connectionStatus = socialConnectionService.describeConnectionState(
                job.getUserId(), job.getChannelId(), socialPlatform.get());
        } else {
            connectionStatus = "NOT_REQUIRED";
        }

        if (job.getStatus() != JobStatus.COMPLETED) {
            return new PublishReadiness(false, "Job is not completed", connectionStatus);
        }
        if (job.getReviewStatus() != ReviewStatus.APPROVED) {
            return new PublishReadiness(false, "Job is not approved", connectionStatus);
        }
        if (!StringUtils.hasText(job.getFinalVideoUrl())) {
            return new PublishReadiness(false, "Final video URL is missing", connectionStatus);
        }
        if (requiresPreferredSelection(job) && !Boolean.TRUE.equals(job.getSelectedForPublish())) {
            return new PublishReadiness(false, "Preferred variant is not selected for publish", connectionStatus);
        }
        if (isTikTokPlatform(platform) && !"ACTIVE".equalsIgnoreCase(connectionStatus)) {
            return new PublishReadiness(false, "Active TikTok connection is required", connectionStatus);
        }
        if (socialPlatform.isPresent() && !"ACTIVE".equalsIgnoreCase(connectionStatus)) {
            return new PublishReadiness(false, "Active " + socialPlatform.get().key() + " connection is required", connectionStatus);
        }
        return new PublishReadiness(true, null, connectionStatus);
    }

    private boolean requiresPreferredSelection(VideoJob job) {
        if (job.getGenerationGroupId() == null) {
            return false;
        }
        return videoJobRepository.countByUserIdAndGenerationGroupId(job.getUserId(), job.getGenerationGroupId()) > 1;
    }

    private boolean isTikTokPlatform(String platform) {
        return StringUtils.hasText(platform) && "tiktok".equalsIgnoreCase(platform.trim());
    }

    private String buildFailureDetails(Exception ex) {
        StringBuilder sb = new StringBuilder();
        sb.append("exception=").append(ex.getClass().getName()).append('\n');
        sb.append("message=").append(ex.getMessage()).append('\n');
        StackTraceElement[] trace = ex.getStackTrace();
        int maxLines = Math.min(10, trace.length);
        for (int i = 0; i < maxLines; i++) {
            sb.append("  at ").append(trace[i]).append('\n');
        }
        return truncate(sb.toString(), 4000);
    }

    private String truncate(String value, int maxLength) {
        if (!StringUtils.hasText(value)) {
            return "Unknown error";
        }
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }

    private String firstNonBlank(String first, String fallback) {
        if (StringUtils.hasText(first)) {
            return first;
        }
        return fallback;
    }

    private String safe(String value) {
        return StringUtils.hasText(value) ? value : "unknown";
    }

    private String buildPublishRequestSnapshot(VideoJob job, String platform, String providerKey, String targetAccountId) {
        return "{\"jobId\":\"" + job.getId() + "\",\"platform\":\"" + platform + "\",\"provider\":\""
            + providerKey + "\",\"targetAccountId\":\"" + safeJson(targetAccountId)
            + "\",\"finalVideoUrl\":\"" + safeJson(truncate(job.getFinalVideoUrl(), 1000)) + "\"}";
    }

    private String safeJson(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private void emitWebhookSafely(WebhookEventType eventType, VideoJob job, Map<String, Object> metadata) {
        try {
            webhookDeliveryService.enqueueJobEvent(eventType, job, metadata == null ? Map.of() : metadata);
        } catch (Exception ex) {
            log.warn(
                "event=webhook_enqueue_failed eventType={} jobId={} message={}",
                eventType.getEventName(),
                job.getId(),
                ex.getMessage()
            );
        }
    }

    private record PublishAttemptContext(
        VideoJob job,
        boolean attemptStarted,
        String platform,
        String providerKey,
        String targetAccountId
    ) {
    }

    private record PublishReadiness(boolean publishable, String reason, String tiktokConnectionStatus) {
    }

    private record AsyncTransition(VideoJob job, boolean changed) {
    }
}
