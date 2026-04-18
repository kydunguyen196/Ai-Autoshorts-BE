package com.autoshorts.ai.controller;

import com.autoshorts.ai.dto.BatchGenerateRequest;
import com.autoshorts.ai.dto.BatchGenerateResponse;
import com.autoshorts.ai.dto.GenerationGroupReviewSummaryResponse;
import com.autoshorts.ai.dto.GenerateVideoRequest;
import com.autoshorts.ai.dto.PagedResponse;
import com.autoshorts.ai.dto.PublishVideoRequest;
import com.autoshorts.ai.dto.RejectVideoRequest;
import com.autoshorts.ai.dto.VideoJobResponse;
import com.autoshorts.ai.dto.VideoPublishStatusResponse;
import com.autoshorts.ai.entity.JobStatus;
import com.autoshorts.ai.entity.WebhookEventType;
import com.autoshorts.ai.service.BatchVideoGenerationService;
import com.autoshorts.ai.service.CurrentUserService;
import com.autoshorts.ai.service.VideoJobDispatchService;
import com.autoshorts.ai.service.VideoPublishService;
import com.autoshorts.ai.service.VideoJobService;
import com.autoshorts.ai.service.WebhookDeliveryService;
import com.autoshorts.ai.util.VideoJobMapper;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/videos")
@Validated
public class VideoGenerationController {

    private static final Logger log = LoggerFactory.getLogger(VideoGenerationController.class);

    private final VideoJobService videoJobService;
    private final VideoJobDispatchService videoJobDispatchService;
    private final BatchVideoGenerationService batchVideoGenerationService;
    private final VideoPublishService videoPublishService;
    private final CurrentUserService currentUserService;
    private final WebhookDeliveryService webhookDeliveryService;

    public VideoGenerationController(
        VideoJobService videoJobService,
        VideoJobDispatchService videoJobDispatchService,
        BatchVideoGenerationService batchVideoGenerationService,
        VideoPublishService videoPublishService,
        CurrentUserService currentUserService,
        WebhookDeliveryService webhookDeliveryService
    ) {
        this.videoJobService = videoJobService;
        this.videoJobDispatchService = videoJobDispatchService;
        this.batchVideoGenerationService = batchVideoGenerationService;
        this.videoPublishService = videoPublishService;
        this.currentUserService = currentUserService;
        this.webhookDeliveryService = webhookDeliveryService;
    }

    @PostMapping("/generate")
    public ResponseEntity<VideoJobResponse> generateVideo(@Valid @RequestBody GenerateVideoRequest request) {
        UUID userId = currentUserService.requireCurrentUserId();
        List<VideoJobResponse> created = videoJobService.createPendingJobVariants(request, userId);

        for (VideoJobResponse response : created) {
            try {
                videoJobDispatchService.publishOrMarkFailed(
                    response.getJobId(),
                    "api_generate",
                    "Job dispatch failed before queue publish"
                );
            } catch (Exception ex) {
                if (created.size() == 1) {
                    throw ex;
                }
                log.warn(
                    "event=multi_variant_dispatch_failed primaryJobId={} failedJobId={} generationGroupId={} message={}",
                    created.get(0).getJobId(),
                    response.getJobId(),
                    response.getGenerationGroupId(),
                    ex.getMessage()
                );
            }
        }

        return ResponseEntity.accepted().body(created.get(0));
    }

    @PostMapping("/batch-generate")
    public ResponseEntity<BatchGenerateResponse> batchGenerate(@Valid @RequestBody BatchGenerateRequest request) {
        UUID userId = currentUserService.requireCurrentUserId();
        return ResponseEntity.accepted().body(batchVideoGenerationService.createBatch(request, userId));
    }

    @GetMapping("/{jobId}")
    public ResponseEntity<VideoJobResponse> getJob(@PathVariable UUID jobId) {
        UUID userId = currentUserService.requireCurrentUserId();
        return ResponseEntity.ok(videoJobService.getJobResponse(jobId, userId));
    }

    @GetMapping
    public ResponseEntity<List<VideoJobResponse>> listRecentJobs(
        @RequestParam(defaultValue = "20") @Min(1) @Max(100) int limit,
        @RequestParam(required = false) JobStatus status
    ) {
        UUID userId = currentUserService.requireCurrentUserId();
        return ResponseEntity.ok(videoJobService.listRecentJobs(userId, limit, status));
    }

    @GetMapping("/feed")
    public ResponseEntity<PagedResponse<VideoJobResponse>> listJobsFeed(
        @RequestParam(defaultValue = "0") @Min(0) int page,
        @RequestParam(defaultValue = "20") @Min(1) @Max(100) int limit,
        @RequestParam(required = false) JobStatus status
    ) {
        UUID userId = currentUserService.requireCurrentUserId();
        return ResponseEntity.ok(videoJobService.listJobsPage(userId, page, limit, status));
    }

    @GetMapping("/group/{groupId}")
    public ResponseEntity<PagedResponse<VideoJobResponse>> listJobsByGroup(
        @PathVariable UUID groupId,
        @RequestParam(defaultValue = "0") @Min(0) int page,
        @RequestParam(defaultValue = "20") @Min(1) @Max(100) int limit,
        @RequestParam(required = false) JobStatus status
    ) {
        UUID userId = currentUserService.requireCurrentUserId();
        return ResponseEntity.ok(videoJobService.listJobsByGroup(userId, groupId, page, limit, status));
    }

    @GetMapping("/group/{groupId}/top-candidates")
    public ResponseEntity<PagedResponse<VideoJobResponse>> listTopCandidatesByGroup(
        @PathVariable UUID groupId,
        @RequestParam(defaultValue = "0") @Min(0) int page,
        @RequestParam(defaultValue = "10") @Min(1) @Max(100) int limit
    ) {
        UUID userId = currentUserService.requireCurrentUserId();
        return ResponseEntity.ok(videoJobService.listTopCandidatesByGroup(userId, groupId, page, limit));
    }

    @GetMapping("/batch/{batchId}")
    public ResponseEntity<PagedResponse<VideoJobResponse>> listJobsByBatch(
        @PathVariable UUID batchId,
        @RequestParam(defaultValue = "0") @Min(0) int page,
        @RequestParam(defaultValue = "20") @Min(1) @Max(100) int limit,
        @RequestParam(required = false) JobStatus status
    ) {
        UUID userId = currentUserService.requireCurrentUserId();
        return ResponseEntity.ok(videoJobService.listJobsByBatch(userId, batchId, page, limit, status));
    }

    @PostMapping("/{jobId}/retry")
    public ResponseEntity<VideoJobResponse> retryJob(@PathVariable UUID jobId) {
        UUID userId = currentUserService.requireCurrentUserId();
        VideoJobResponse response = VideoJobMapper.toResponse(videoJobService.resetFailedForRetry(jobId, userId));
        videoJobDispatchService.publishOrMarkFailed(
            jobId,
            "api_retry",
            "Retry dispatch failed before queue publish"
        );
        return ResponseEntity.accepted().body(response);
    }

    @PostMapping("/{jobId}/approve")
    public ResponseEntity<VideoJobResponse> approveJob(@PathVariable UUID jobId) {
        UUID userId = currentUserService.requireCurrentUserId();
        VideoJobResponse response = VideoJobMapper.toResponse(videoJobService.approveJobForUser(jobId, userId));
        emitWebhookSafely(WebhookEventType.JOB_APPROVED, jobId, userId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{jobId}/reject")
    public ResponseEntity<VideoJobResponse> rejectJob(
        @PathVariable UUID jobId,
        @Valid @RequestBody RejectVideoRequest request
    ) {
        UUID userId = currentUserService.requireCurrentUserId();
        VideoJobResponse response = VideoJobMapper.toResponse(
            videoJobService.rejectJobForUser(jobId, userId, request.getRejectionReason())
        );
        emitWebhookSafely(WebhookEventType.JOB_REJECTED, jobId, userId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{jobId}/select-for-publish")
    public ResponseEntity<VideoJobResponse> selectForPublish(@PathVariable UUID jobId) {
        UUID userId = currentUserService.requireCurrentUserId();
        VideoJobResponse response = VideoJobMapper.toResponse(videoJobService.selectForPublishForUser(jobId, userId));
        return ResponseEntity.ok(response);
    }

    @GetMapping("/group/{groupId}/review-summary")
    public ResponseEntity<GenerationGroupReviewSummaryResponse> getGroupReviewSummary(@PathVariable UUID groupId) {
        UUID userId = currentUserService.requireCurrentUserId();
        return ResponseEntity.ok(videoJobService.getGroupReviewSummary(userId, groupId));
    }

    @PostMapping("/{jobId}/publish")
    public ResponseEntity<VideoJobResponse> publishJob(
        @PathVariable UUID jobId,
        @RequestBody(required = false) PublishVideoRequest request
    ) {
        UUID userId = currentUserService.requireCurrentUserId();
        String publishPlatform = request == null ? null : request.getPublishPlatform();
        return ResponseEntity.ok(videoPublishService.publish(jobId, userId, publishPlatform));
    }

    @GetMapping("/{jobId}/publish")
    public ResponseEntity<VideoPublishStatusResponse> getPublishStatus(@PathVariable UUID jobId) {
        UUID userId = currentUserService.requireCurrentUserId();
        return ResponseEntity.ok(videoPublishService.getPublishStatus(jobId, userId));
    }

    private void emitWebhookSafely(WebhookEventType eventType, UUID jobId, UUID userId) {
        try {
            webhookDeliveryService.enqueueJobEvent(eventType, videoJobService.getEntityForUserOrThrow(jobId, userId));
        } catch (Exception ex) {
            log.warn(
                "event=webhook_enqueue_failed eventType={} jobId={} message={}",
                eventType.getEventName(),
                jobId,
                ex.getMessage()
            );
        }
    }
}
