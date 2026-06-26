package com.autoshorts.ai.orchestration;

import com.autoshorts.ai.audio.DeterministicPreviewAudioGenerator;
import com.autoshorts.ai.client.ElevenLabsClient;
import com.autoshorts.ai.client.model.SynthesizedAudio;
import com.autoshorts.ai.config.AppProperties;
import com.autoshorts.ai.entity.GenerationStep;
import com.autoshorts.ai.entity.NotificationType;
import com.autoshorts.ai.entity.VideoJob;
import com.autoshorts.ai.entity.WebhookEventType;
import com.autoshorts.ai.ffmpeg.AudioDurationResolver;
import com.autoshorts.ai.exception.InvalidJobStateException;
import com.autoshorts.ai.ffmpeg.FfmpegVideoComposer;
import com.autoshorts.ai.ffmpeg.SceneMediaSegment;
import com.autoshorts.ai.ffmpeg.VideoCompositionRequest;
import com.autoshorts.ai.metrics.PipelineMetrics;
import com.autoshorts.ai.service.ContentGenerationService;
import com.autoshorts.ai.service.NotificationService;
import com.autoshorts.ai.service.VideoPublishService;
import com.autoshorts.ai.service.VisualAssetGenerationService;
import com.autoshorts.ai.service.VideoJobService;
import com.autoshorts.ai.service.WebhookDeliveryService;
import com.autoshorts.ai.service.model.GeneratedContent;
import com.autoshorts.ai.storage.StorageClient;
import com.autoshorts.ai.subtitles.SubtitleGeneratorService;
import com.autoshorts.ai.util.WorkingDirectoryManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class VideoGenerationOrchestrator {

    private static final Logger log = LoggerFactory.getLogger(VideoGenerationOrchestrator.class);

    private final VideoJobService videoJobService;
    private final ContentGenerationService contentGenerationService;
    private final ElevenLabsClient elevenLabsClient;
    private final DeterministicPreviewAudioGenerator previewAudioGenerator;
    private final SubtitleGeneratorService subtitleGeneratorService;
    private final VisualAssetGenerationService visualAssetGenerationService;
    private final FfmpegVideoComposer ffmpegVideoComposer;
    private final AudioDurationResolver audioDurationResolver;
    private final StorageClient storageClient;
    private final WebhookDeliveryService webhookDeliveryService;
    private final WorkingDirectoryManager workingDirectoryManager;
    private final AppProperties appProperties;
    private final NotificationService notificationService;
    private final VideoPublishService videoPublishService;
    private final PipelineMetrics pipelineMetrics;

    public VideoGenerationOrchestrator(
        VideoJobService videoJobService,
        ContentGenerationService contentGenerationService,
        ElevenLabsClient elevenLabsClient,
        DeterministicPreviewAudioGenerator previewAudioGenerator,
        SubtitleGeneratorService subtitleGeneratorService,
        VisualAssetGenerationService visualAssetGenerationService,
        FfmpegVideoComposer ffmpegVideoComposer,
        AudioDurationResolver audioDurationResolver,
        StorageClient storageClient,
        WebhookDeliveryService webhookDeliveryService,
        WorkingDirectoryManager workingDirectoryManager,
        AppProperties appProperties,
        NotificationService notificationService,
        VideoPublishService videoPublishService,
        PipelineMetrics pipelineMetrics
    ) {
        this.videoJobService = videoJobService;
        this.contentGenerationService = contentGenerationService;
        this.elevenLabsClient = elevenLabsClient;
        this.previewAudioGenerator = previewAudioGenerator;
        this.subtitleGeneratorService = subtitleGeneratorService;
        this.visualAssetGenerationService = visualAssetGenerationService;
        this.ffmpegVideoComposer = ffmpegVideoComposer;
        this.audioDurationResolver = audioDurationResolver;
        this.storageClient = storageClient;
        this.webhookDeliveryService = webhookDeliveryService;
        this.workingDirectoryManager = workingDirectoryManager;
        this.appProperties = appProperties;
        this.notificationService = notificationService;
        this.videoPublishService = videoPublishService;
        this.pipelineMetrics = pipelineMetrics;
    }

    public void processJob(UUID jobId) {
        Path jobDir = null;
        boolean success = false;
        // Mutable holder so failures inside runRenderPipeline still report the exact failed step.
        final GenerationStep[] stepRef = { GenerationStep.QUEUED };
        log.info("event=job_processing_requested jobId={}", jobId);
        try {
            videoJobService.startProcessing(jobId);
        } catch (InvalidJobStateException ex) {
            log.info("event=job_processing_skipped jobId={} reason={}", jobId, ex.getMessage());
            return;
        }

        try {
            VideoJob job = videoJobService.getEntityOrThrow(jobId);
            jobDir = workingDirectoryManager.createJobDirectory(jobId);
            final Path activeJobDir = jobDir;

            // Two-phase pipeline (Phase 5): when reviewBeforeRender is set, the first pass stops
            // after content generation at AWAITING_REVIEW for the user to edit; the finalize pass
            // (renderApproved=true) skips content and renders the (possibly edited) script.
            boolean finalizing = Boolean.TRUE.equals(job.getRenderApproved());

            String script;
            if (finalizing) {
                script = job.getScriptText();
                log.info("event=job_finalize_resume jobId={}", jobId);
            } else {
                stepRef[0] = GenerationStep.CONTENT_PREPARATION;
                GeneratedContent generatedContent = runStep(
                    jobId,
                    stepRef[0],
                    () -> contentGenerationService.generateForJob(job)
                );
                persistGeneratedContent(jobId, job, generatedContent);
                script = generatedContent.scriptText();

                if (Boolean.TRUE.equals(job.getReviewBeforeRender())) {
                    VideoJob awaiting = videoJobService.markAwaitingReview(jobId);
                    emitWebhookSafely(WebhookEventType.JOB_GENERATED, awaiting);
                    notifySafely(
                        awaiting.getUserId(),
                        NotificationType.JOB_COMPLETED,
                        "Bản nháp sẵn sàng để duyệt",
                        "Kịch bản cho \"" + awaiting.getTopic() + "\" đã tạo xong. Hãy duyệt/chỉnh sửa rồi hoàn tất để dựng video.",
                        jobId
                    );
                    log.info("event=job_awaiting_review jobId={}", jobId);
                    success = true;
                    pipelineMetrics.recordJobOutcome("awaiting_review", null);
                    return;
                }
            }

            runRenderPipeline(jobId, job, script, activeJobDir, stepRef);
            success = true;
            pipelineMetrics.recordJobOutcome("completed", null);
        } catch (Exception ex) {
            GenerationStep currentStep = stepRef[0];
            pipelineMetrics.recordJobOutcome("failed", currentStep);
            io.sentry.Sentry.captureException(ex);
            log.error("event=job_failed jobId={} step={} message={}", jobId, currentStep, ex.getMessage(), ex);
            String userFacingMessage = buildUserFacingErrorMessage(currentStep, ex);
            String stepDetails = buildStepErrorDetails(currentStep, userFacingMessage, ex);
            try {
                VideoJob failedJob = videoJobService.markFailed(jobId, currentStep, truncate(userFacingMessage, 500), truncate(stepDetails, 8000));
                emitWebhookSafely(WebhookEventType.JOB_FAILED, failedJob);
                notifySafely(
                    failedJob.getUserId(),
                    NotificationType.JOB_FAILED,
                    "Tạo video thất bại",
                    "Video cho chủ đề \"" + failedJob.getTopic() + "\" thất bại: " + truncate(userFacingMessage, 200),
                    jobId
                );
            } catch (Exception markFailedEx) {
                log.error("event=job_mark_failed_error jobId={} message={}", jobId, markFailedEx.getMessage(), markFailedEx);
            }
        } finally {
            workingDirectoryManager.cleanupJobDirectory(jobDir, !success);
        }
    }

    /** Render pipeline: audio → subtitles → visuals → composition → COMPLETED. */
    private void runRenderPipeline(UUID jobId, VideoJob job, String script, Path activeJobDir, GenerationStep[] stepRef)
        throws Exception {
        stepRef[0] = GenerationStep.AUDIO_SYNTHESIS;
        AudioArtifact audioArtifact = runStep(jobId, stepRef[0], () -> synthesizeAndUploadAudio(job, script, activeJobDir));
        int effectiveDurationSeconds = resolveEffectiveDurationSeconds(audioArtifact.audioPath(), job.getDurationSeconds());
        videoJobService.updateJob(jobId, current -> {
            current.setAudioUrl(audioArtifact.audioUrl());
            current.setAudioGenerationMode(audioArtifact.mode());
            current.setAudioProvider(audioArtifact.provider());
        });

        stepRef[0] = GenerationStep.SUBTITLE_GENERATION;
        SubtitleArtifact subtitleArtifact = runStep(
            jobId,
            stepRef[0],
            () -> generateAndUploadSubtitles(job, script, activeJobDir, effectiveDurationSeconds)
        );
        videoJobService.updateJob(jobId, current -> current.setSubtitleUrl(subtitleArtifact.subtitleUrl()));

        stepRef[0] = GenerationStep.VISUAL_ASSET_GENERATION;
        VisualAssetGenerationService.GeneratedVisualAssets visualAssets = runStep(
            jobId,
            stepRef[0],
            () -> visualAssetGenerationService.generateAndUploadSceneAssets(job, activeJobDir, effectiveDurationSeconds)
        );
        videoJobService.updateJob(jobId, current -> {
            current.setSceneAssetsJson(visualAssets.sceneAssetsJson());
            current.setVisualGenerationMode(visualAssets.mode());
            current.setVisualProvider(visualAssets.provider());
            current.setVisualModelId(visualAssets.modelId());
            current.setVisualFailureReason(visualAssets.failureReason());
            current.setVisualFailureDetails(visualAssets.failureDetails());
        });

        stepRef[0] = GenerationStep.VIDEO_COMPOSITION;
        String finalVideoUrl = runStep(
            jobId,
            stepRef[0],
            () -> composeAndUploadVideo(
                job,
                audioArtifact.audioPath(),
                subtitleArtifact.subtitlePath(),
                visualAssets.sceneSegments(),
                activeJobDir,
                effectiveDurationSeconds
            )
        );
        VideoJob completedJob = videoJobService.markCompleted(jobId, finalVideoUrl);
        emitWebhookSafely(WebhookEventType.JOB_GENERATED, completedJob);
        emitWebhookSafely(WebhookEventType.JOB_COMPLETED, completedJob);
        notifySafely(
            completedJob.getUserId(),
            NotificationType.JOB_COMPLETED,
            "Video đã sẵn sàng",
            "Video cho chủ đề \"" + completedJob.getTopic() + "\" đã tạo xong.",
            jobId
        );
        autoPublishIfRequested(completedJob);
        log.info("event=job_completed jobId={} finalVideoUrl={}", jobId, finalVideoUrl);
    }

    private void persistGeneratedContent(UUID jobId, VideoJob job, GeneratedContent generatedContent) {
            videoJobService.updateJob(jobId, current -> {
                current.setResolvedStyle(generatedContent.resolvedStyle());
                current.setPromptTemplateId(generatedContent.promptTemplateId());
                current.setContentGenerationMode(generatedContent.generationMode());
                current.setContentVariantKey(generatedContent.styleVariantKey());
                current.setHookStrategy(generatedContent.hookStrategy());
                current.setCtaStrategy(generatedContent.ctaStrategy());
                current.setStructureStrategy(generatedContent.structureStrategy());
                current.setHookStrengthScore(generatedContent.hookStrengthScore());
                current.setEngagementScore(generatedContent.engagementScore());
                current.setEngagementTagsJson(generatedContent.engagementTagsJson());
                current.setHookText(generatedContent.hookText());
                current.setScriptText(generatedContent.scriptText());
                current.setCtaText(generatedContent.ctaText());
                current.setCaptionText(generatedContent.captionText());
                current.setHashtags(joinHashtags(generatedContent.hashtags()));
                current.setSceneBreakdownJson(generatedContent.sceneBreakdownJson());
            });
            job.setSceneBreakdownJson(generatedContent.sceneBreakdownJson());
            job.setScriptText(generatedContent.scriptText());
            log.info(
                "event=content_metadata_persisted jobId={} mode={} style={} templateId={} variantKey={} hookType={} ctaType={} structureType={} hookScore={} engagementScore={}",
                jobId,
                generatedContent.generationMode(),
                generatedContent.resolvedStyle(),
                generatedContent.promptTemplateId(),
                generatedContent.styleVariantKey(),
                generatedContent.hookStrategy(),
                generatedContent.ctaStrategy(),
                generatedContent.structureStrategy(),
                generatedContent.hookStrengthScore(),
                generatedContent.engagementScore()
            );
            if (job.getGenerationGroupId() != null) {
                videoJobService.recomputeRankingForGroup(job.getUserId(), job.getGenerationGroupId());
            }
    }

    private AudioArtifact synthesizeAndUploadAudio(VideoJob job, String script, Path jobDir) throws IOException {
        log.info("event=step_generate_audio jobId={}", job.getId());
        SynthesizedAudio audio;
        try {
            audio = elevenLabsClient.synthesizeSpeech(script, job.getVoiceId(), job.getDurationSeconds());
        } catch (Exception ex) {
            log.warn(
                "event=audio_synthesis_fallback_triggered jobId={} reason=client_exception message={}",
                job.getId(),
                ex.getMessage()
            );
            audio = previewAudioGenerator.generateFallbackSpeech(script, job.getDurationSeconds(), "client_exception");
        }

        String fileName = "audio." + audio.getFileExtension();
        Path audioPath = jobDir.resolve(fileName);
        Files.write(audioPath, audio.getData());

        String objectKey = objectKey(job.getId(), fileName);
        String audioUrl = storageClient.upload(audioPath, objectKey, audio.getContentType());
        return new AudioArtifact(audioPath, audioUrl, audio.getGenerationMode(), audio.getProvider());
    }

    private SubtitleArtifact generateAndUploadSubtitles(
        VideoJob job,
        String script,
        Path jobDir,
        int effectiveDurationSeconds
    ) {
        log.info("event=step_generate_subtitles jobId={}", job.getId());
        Path subtitlePath = jobDir.resolve("subtitles.srt");
        subtitleGeneratorService.generateSrt(script, effectiveDurationSeconds, subtitlePath);

        String subtitleUrl = storageClient.upload(
            subtitlePath,
            objectKey(job.getId(), "subtitles.srt"),
            "application/x-subrip"
        );
        return new SubtitleArtifact(subtitlePath, subtitleUrl);
    }

    private String composeAndUploadVideo(
        VideoJob job,
        Path audioPath,
        Path subtitlePath,
        List<SceneMediaSegment> sceneSegments,
        Path jobDir,
        int effectiveDurationSeconds
    ) {
        Path outputPath = jobDir.resolve("final.mp4");
        Path backgroundPath = resolveBackgroundPath();

        VideoCompositionRequest request = new VideoCompositionRequest(
            backgroundPath,
            audioPath,
            subtitlePath,
            outputPath,
            effectiveDurationSeconds,
            null,
            sceneSegments
        );
        ffmpegVideoComposer.compose(request);

        return storageClient.upload(outputPath, objectKey(job.getId(), "final.mp4"), "video/mp4");
    }

    private int resolveEffectiveDurationSeconds(Path audioPath, Integer fallbackDurationSeconds) {
        int fallback = fallbackDurationSeconds == null ? 30 : fallbackDurationSeconds;
        return audioDurationResolver.resolveDurationSeconds(audioPath, fallback);
    }

    private Path resolveBackgroundPath() {
        Path configured = Path.of(appProperties.getAssets().getDefaultBackgroundPath());
        if (configured.isAbsolute()) {
            return configured.normalize();
        }
        return Path.of("").toAbsolutePath().resolve(configured).normalize();
    }

    private String objectKey(UUID jobId, String fileName) {
        return "jobs/" + jobId + "/" + fileName;
    }

    private String joinHashtags(java.util.List<String> hashtags) {
        if (hashtags == null || hashtags.isEmpty()) {
            return null;
        }
        return String.join(",", hashtags);
    }

    private <T> T runStep(UUID jobId, GenerationStep step, StepSupplier<T> stepSupplier) throws Exception {
        videoJobService.markCurrentStep(jobId, step);
        long startedAt = System.currentTimeMillis();
        log.info("event=job_step_start jobId={} step={}", jobId, step);
        try {
            T result = stepSupplier.get();
            long elapsed = System.currentTimeMillis() - startedAt;
            pipelineMetrics.recordStep(step, "success", elapsed);
            log.info("event=job_step_success jobId={} step={} elapsedMs={}", jobId, step, elapsed);
            return result;
        } catch (Exception ex) {
            long elapsed = System.currentTimeMillis() - startedAt;
            pipelineMetrics.recordStep(step, "failure", elapsed);
            log.error("event=job_step_failed jobId={} step={} elapsedMs={} message={}", jobId, step, elapsed, ex.getMessage());
            throw ex;
        }
    }

    private String buildUserFacingErrorMessage(GenerationStep step, Exception ex) {
        String rootCause = rootCauseMessage(ex);
        String stepLabel = stepLabel(step);
        if (rootCause == null || rootCause.isBlank()) {
            return stepLabel + " failed. Please retry the job or check service logs if it fails again.";
        }
        return stepLabel + " failed: " + rootCause;
    }

    private String stepLabel(GenerationStep step) {
        if (step == null) {
            return "Video generation";
        }
        return switch (step) {
            case QUEUED -> "Queue handoff";
            case CONTENT_PREPARATION, SCRIPT_GENERATION -> "Script/content generation";
            case AUDIO_SYNTHESIS -> "Audio synthesis";
            case SUBTITLE_GENERATION -> "Subtitle generation";
            case VISUAL_ASSET_GENERATION -> "Visual asset generation";
            case VIDEO_COMPOSITION -> "Video composition";
            case COMPLETED -> "Completion finalization";
        };
    }

    private String buildStepErrorDetails(GenerationStep step, String userFacingMessage, Exception ex) {
        StringBuilder sb = new StringBuilder();
        sb.append("occurredAt=").append(Instant.now()).append('\n');
        sb.append("step=").append(step).append('\n');
        sb.append("userMessage=").append(userFacingMessage).append('\n');
        sb.append("exception=").append(ex.getClass().getName()).append('\n');
        sb.append("message=").append(nullSafe(ex.getMessage())).append('\n');
        Throwable rootCause = rootCause(ex);
        if (rootCause != ex) {
            sb.append("rootCauseException=").append(rootCause.getClass().getName()).append('\n');
            sb.append("rootCauseMessage=").append(nullSafe(rootCause.getMessage())).append('\n');
        }
        sb.append("stacktrace=").append('\n');
        StackTraceElement[] trace = ex.getStackTrace();
        int maxLines = Math.min(trace.length, 20);
        for (int i = 0; i < maxLines; i++) {
            sb.append("  at ").append(trace[i]).append('\n');
        }
        return sb.toString();
    }

    private Throwable rootCause(Throwable throwable) {
        Throwable cursor = throwable;
        while (cursor.getCause() != null && cursor.getCause() != cursor) {
            cursor = cursor.getCause();
        }
        return cursor;
    }

    private String rootCauseMessage(Throwable throwable) {
        return nullSafe(rootCause(throwable).getMessage());
    }

    private String nullSafe(String value) {
        return value == null ? "Unknown error" : value;
    }

    private String truncate(String value, int maxLength) {
        if (value == null) {
            return "Unknown error";
        }
        if (value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }

    private void emitWebhookSafely(WebhookEventType eventType, VideoJob job) {
        try {
            webhookDeliveryService.enqueueJobEvent(eventType, job);
        } catch (Exception ex) {
            log.warn(
                "event=webhook_enqueue_failed eventType={} jobId={} message={}",
                eventType.getEventName(),
                job.getId(),
                ex.getMessage()
            );
        }
    }

    private void notifySafely(UUID userId, NotificationType type, String title, String message, UUID jobId) {
        try {
            notificationService.notify(userId, type, title, message, java.util.Map.of("jobId", jobId.toString()));
        } catch (Exception ex) {
            log.warn("event=notification_emit_failed type={} jobId={} message={}", type, jobId, ex.getMessage());
        }
    }

    private void autoPublishIfRequested(VideoJob completedJob) {
        if (!Boolean.TRUE.equals(completedJob.getAutoPublish())) {
            return;
        }
        try {
            videoPublishService.publish(completedJob.getId(), completedJob.getUserId(), null);
            notifySafely(
                completedJob.getUserId(),
                NotificationType.PUBLISH_DONE,
                "Đã đăng video tự động",
                "Video cho chủ đề \"" + completedJob.getTopic() + "\" đã được gửi đăng.",
                completedJob.getId()
            );
            log.info("event=auto_publish_triggered jobId={}", completedJob.getId());
        } catch (Exception ex) {
            log.warn("event=auto_publish_failed jobId={} message={}", completedJob.getId(), ex.getMessage());
            notifySafely(
                completedJob.getUserId(),
                NotificationType.PUBLISH_FAILED,
                "Đăng video tự động thất bại",
                "Không thể tự động đăng video cho \"" + completedJob.getTopic() + "\": " + truncate(ex.getMessage(), 200),
                completedJob.getId()
            );
        }
    }

    private record AudioArtifact(Path audioPath, String audioUrl, com.autoshorts.ai.entity.AudioGenerationMode mode, String provider) {
    }

    private record SubtitleArtifact(Path subtitlePath, String subtitleUrl) {
    }

    @FunctionalInterface
    private interface StepSupplier<T> {
        T get() throws Exception;
    }
}
