package com.autoshorts.ai.service;

import com.autoshorts.ai.dto.BatchGenerateItemRequest;
import com.autoshorts.ai.dto.BatchGenerateJobResult;
import com.autoshorts.ai.dto.BatchGenerateRequest;
import com.autoshorts.ai.dto.BatchGenerateResponse;
import com.autoshorts.ai.dto.GenerateVideoRequest;
import com.autoshorts.ai.dto.VideoJobResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
public class BatchVideoGenerationService {

    private static final Logger log = LoggerFactory.getLogger(BatchVideoGenerationService.class);

    private final VideoJobService videoJobService;
    private final VideoJobDispatchService videoJobDispatchService;
    private final ChannelService channelService;

    public BatchVideoGenerationService(
        VideoJobService videoJobService,
        VideoJobDispatchService videoJobDispatchService,
        ChannelService channelService
    ) {
        this.videoJobService = videoJobService;
        this.videoJobDispatchService = videoJobDispatchService;
        this.channelService = channelService;
    }

    public BatchGenerateResponse createBatch(BatchGenerateRequest request, UUID userId) {
        UUID batchId = UUID.randomUUID();
        List<BatchGenerateJobResult> results = new ArrayList<>();
        Set<String> dedupe = new HashSet<>();
        int totalVariantsRequested = 0;

        for (BatchGenerateItemRequest item : request.getItems()) {
            GenerateVideoRequest single = null;
            UUID channelId = null;
            try {
                single = toGenerateRequest(request, item);
                channelId = channelService.resolveOwnedChannelIdOrDefault(userId, single.getChannelId());
                int variantCount = normalizeVariantCount(single.getVariantCount());
                totalVariantsRequested += variantCount;

                String resolvedStyle = firstNonBlank(single.getContentStyle(), single.getStyle(), "motivation");
                String dedupeKey = normalize(single.getTopic()) + "|" + normalize(resolvedStyle) + "|" + channelId;
                if (!dedupe.add(dedupeKey)) {
                    BatchGenerateJobResult duplicate = buildErrorResult(
                        item.getTopic(),
                        batchId,
                        null,
                        "Duplicate topic/style item in batch request"
                    );
                    results.add(duplicate);
                    log.info(
                        "event=batch_job_skipped reason=duplicate_in_payload batchId={} topic={} style={}",
                        batchId,
                        single.getTopic(),
                        resolvedStyle
                    );
                    continue;
                }

                if (videoJobService.hasActiveJobFor(single.getTopic(), resolvedStyle, userId, channelId)) {
                    BatchGenerateJobResult skipped = buildErrorResult(
                        item.getTopic(),
                        batchId,
                        null,
                        "Active job already exists for the same topic/style"
                    );
                    results.add(skipped);
                    log.info(
                        "event=batch_job_skipped reason=active_job_exists batchId={} userId={} channelId={} topic={} style={}",
                        batchId,
                        userId,
                        channelId,
                        single.getTopic(),
                        resolvedStyle
                    );
                    continue;
                }

                UUID groupId = UUID.randomUUID();
                List<VideoJobResponse> createdJobs = videoJobService.createPendingJobVariants(
                    single,
                    userId,
                    channelId,
                    batchId,
                    groupId,
                    variantCount
                );

                for (VideoJobResponse created : createdJobs) {
                    BatchGenerateJobResult result = new BatchGenerateJobResult();
                    result.setBatchId(batchId);
                    result.setGenerationGroupId(created.getGenerationGroupId());
                    result.setVariantIndex(created.getVariantIndex());
                    result.setVariantCount(created.getVariantCount());
                    result.setTopic(created.getTopic());
                    result.setJobId(created.getJobId());
                    result.setStatus(created.getStatus());

                    try {
                        videoJobDispatchService.publishOrMarkFailed(
                            created.getJobId(),
                            "batch_generate",
                            "Batch dispatch failed before queue publish"
                        );
                    } catch (Exception dispatchEx) {
                        result.setErrorMessage(dispatchEx.getMessage());
                    }

                    results.add(result);
                    log.info(
                        "event=batch_variant_created batchId={} generationGroupId={} jobId={} variantIndex={} variantCount={} topic={}",
                        batchId,
                        created.getGenerationGroupId(),
                        created.getJobId(),
                        created.getVariantIndex(),
                        created.getVariantCount(),
                        created.getTopic()
                    );
                }
            } catch (Exception ex) {
                BatchGenerateJobResult failed = buildErrorResult(
                    item.getTopic(),
                    batchId,
                    null,
                    ex.getMessage()
                );
                results.add(failed);
                log.error(
                    "event=batch_job_create_failed batchId={} topic={} message={}",
                    batchId,
                    item.getTopic(),
                    ex.getMessage(),
                    ex
                );
            }
        }

        BatchGenerateResponse response = new BatchGenerateResponse();
        response.setBatchId(batchId);
        response.setTotalRequested(request.getItems().size());
        response.setTotalVariantsRequested(totalVariantsRequested);
        response.setTotalAccepted((int) results.stream().filter(r -> r.getJobId() != null).count());
        response.setCreatedAt(Instant.now());
        response.setJobs(results);

        log.info(
            "event=batch_generation_submitted batchId={} totalRequested={} totalVariantsRequested={} totalAccepted={}",
            response.getBatchId(),
            response.getTotalRequested(),
            response.getTotalVariantsRequested(),
            response.getTotalAccepted()
        );
        return response;
    }

    private BatchGenerateJobResult buildErrorResult(String topic, UUID batchId, UUID groupId, String message) {
        BatchGenerateJobResult result = new BatchGenerateJobResult();
        result.setBatchId(batchId);
        result.setGenerationGroupId(groupId);
        result.setTopic(topic);
        result.setErrorMessage(message);
        return result;
    }

    private GenerateVideoRequest toGenerateRequest(BatchGenerateRequest batch, BatchGenerateItemRequest item) {
        GenerateVideoRequest request = new GenerateVideoRequest();
        request.setTopic(item.getTopic());
        request.setStyle(firstNonBlank(item.getStyle(), batch.getDefaultStyle(), "motivation"));
        request.setContentStyle(firstNonBlank(item.getContentStyle(), batch.getDefaultContentStyle(), null));
        request.setVoiceId(firstNonBlank(item.getVoiceId(), batch.getDefaultVoiceId(), null));
        request.setChannelId(item.getChannelId() != null ? item.getChannelId() : batch.getDefaultChannelId());
        request.setCharacterProfileId(
            item.getCharacterProfileId() != null ? item.getCharacterProfileId() : batch.getDefaultCharacterProfileId()
        );
        request.setCharacterCampaignId(
            item.getCharacterCampaignId() != null ? item.getCharacterCampaignId() : batch.getDefaultCharacterCampaignId()
        );
        request.setStoryAngle(firstNonBlank(item.getStoryAngle(), batch.getDefaultStoryAngle(), null));
        request.setProductPlacementMode(firstNonBlank(item.getProductPlacementMode(), batch.getDefaultProductPlacementMode(), null));
        request.setAdDisclosureMode(firstNonBlank(item.getAdDisclosureMode(), batch.getDefaultAdDisclosureMode(), null));
        request.setSceneCountTarget(
            item.getSceneCountTarget() != null ? item.getSceneCountTarget() : batch.getDefaultSceneCountTarget()
        );
        request.setCharacterConsistencyMode(
            firstNonBlank(item.getCharacterConsistencyMode(), batch.getDefaultCharacterConsistencyMode(), null)
        );
        request.setDurationSeconds(item.getDurationSeconds() != null ? item.getDurationSeconds() : batch.getDefaultDurationSeconds());
        request.setVariantCount(item.getVariantCount() != null ? item.getVariantCount() : batch.getDefaultVariantCount());
        return request;
    }

    private String firstNonBlank(String first, String second, String fallback) {
        if (StringUtils.hasText(first)) {
            return first.trim();
        }
        if (StringUtils.hasText(second)) {
            return second.trim();
        }
        return fallback;
    }

    private int normalizeVariantCount(Integer variantCount) {
        if (variantCount == null) {
            return 1;
        }
        return Math.max(1, Math.min(10, variantCount));
    }

    private String normalize(String value) {
        if (!StringUtils.hasText(value)) {
            return "";
        }
        return value.trim().toLowerCase(Locale.ROOT);
    }
}
