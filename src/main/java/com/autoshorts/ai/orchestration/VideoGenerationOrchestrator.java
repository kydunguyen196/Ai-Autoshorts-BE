package com.autoshorts.ai.orchestration;

import com.autoshorts.ai.audio.DeterministicPreviewAudioGenerator;
import com.autoshorts.ai.client.ElevenLabsClient;
import com.autoshorts.ai.client.model.SynthesizedAudio;
import com.autoshorts.ai.config.AppProperties;
import com.autoshorts.ai.entity.GenerationStep;
import com.autoshorts.ai.entity.VideoJob;
import com.autoshorts.ai.entity.WebhookEventType;
import com.autoshorts.ai.exception.InvalidJobStateException;
import com.autoshorts.ai.ffmpeg.FfmpegVideoComposer;
import com.autoshorts.ai.ffmpeg.VideoCompositionRequest;
import com.autoshorts.ai.service.ContentGenerationService;
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
import java.util.UUID;

@Service
public class VideoGenerationOrchestrator {

    private static final Logger log = LoggerFactory.getLogger(VideoGenerationOrchestrator.class);

    private final VideoJobService videoJobService;
    private final ContentGenerationService contentGenerationService;
    private final ElevenLabsClient elevenLabsClient;
    private final DeterministicPreviewAudioGenerator previewAudioGenerator;
    private final SubtitleGeneratorService subtitleGeneratorService;
    private final FfmpegVideoComposer ffmpegVideoComposer;
    private final StorageClient storageClient;
    private final WebhookDeliveryService webhookDeliveryService;
    private final WorkingDirectoryManager workingDirectoryManager;
    private final AppProperties appProperties;

    public VideoGenerationOrchestrator(
        VideoJobService videoJobService,
        ContentGenerationService contentGenerationService,
        ElevenLabsClient elevenLabsClient,
        DeterministicPreviewAudioGenerator previewAudioGenerator,
        SubtitleGeneratorService subtitleGeneratorService,
        FfmpegVideoComposer ffmpegVideoComposer,
        StorageClient storageClient,
        WebhookDeliveryService webhookDeliveryService,
        WorkingDirectoryManager workingDirectoryManager,
        AppProperties appProperties
    ) {
        this.videoJobService = videoJobService;
        this.contentGenerationService = contentGenerationService;
        this.elevenLabsClient = elevenLabsClient;
        this.previewAudioGenerator = previewAudioGenerator;
        this.subtitleGeneratorService = subtitleGeneratorService;
        this.ffmpegVideoComposer = ffmpegVideoComposer;
        this.storageClient = storageClient;
        this.webhookDeliveryService = webhookDeliveryService;
        this.workingDirectoryManager = workingDirectoryManager;
        this.appProperties = appProperties;
    }

    public void processJob(UUID jobId) {
        Path jobDir = null;
        boolean success = false;
        GenerationStep currentStep = GenerationStep.QUEUED;
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

            currentStep = GenerationStep.CONTENT_PREPARATION;
            GeneratedContent generatedContent = runStep(
                jobId,
                currentStep,
                () -> contentGenerationService.generateForJob(job)
            );
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
            String script = generatedContent.scriptText();

            currentStep = GenerationStep.AUDIO_SYNTHESIS;
            AudioArtifact audioArtifact = runStep(jobId, currentStep, () -> synthesizeAndUploadAudio(job, script, activeJobDir));
            videoJobService.updateJob(jobId, current -> {
                current.setAudioUrl(audioArtifact.audioUrl());
                current.setAudioGenerationMode(audioArtifact.mode());
                current.setAudioProvider(audioArtifact.provider());
            });

            currentStep = GenerationStep.SUBTITLE_GENERATION;
            SubtitleArtifact subtitleArtifact = runStep(jobId, currentStep, () -> generateAndUploadSubtitles(job, script, activeJobDir));
            videoJobService.updateJob(jobId, current -> current.setSubtitleUrl(subtitleArtifact.subtitleUrl()));

            currentStep = GenerationStep.VIDEO_COMPOSITION;
            String finalVideoUrl = runStep(
                jobId,
                currentStep,
                () -> composeAndUploadVideo(job, audioArtifact.audioPath(), subtitleArtifact.subtitlePath(), activeJobDir)
            );
            VideoJob completedJob = videoJobService.markCompleted(jobId, finalVideoUrl);
            emitWebhookSafely(WebhookEventType.JOB_GENERATED, completedJob);
            emitWebhookSafely(WebhookEventType.JOB_COMPLETED, completedJob);

            log.info("event=job_completed jobId={} finalVideoUrl={}", jobId, finalVideoUrl);
            success = true;
        } catch (Exception ex) {
            log.error("event=job_failed jobId={} message={}", jobId, ex.getMessage(), ex);
            String stepDetails = buildStepErrorDetails(currentStep, ex);
            try {
                VideoJob failedJob = videoJobService.markFailed(jobId, currentStep, truncate(ex.getMessage(), 500), truncate(stepDetails, 8000));
                emitWebhookSafely(WebhookEventType.JOB_FAILED, failedJob);
            } catch (Exception markFailedEx) {
                log.error("event=job_mark_failed_error jobId={} message={}", jobId, markFailedEx.getMessage(), markFailedEx);
            }
        } finally {
            workingDirectoryManager.cleanupJobDirectory(jobDir, !success);
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

    private SubtitleArtifact generateAndUploadSubtitles(VideoJob job, String script, Path jobDir) {
        log.info("event=step_generate_subtitles jobId={}", job.getId());
        Path subtitlePath = jobDir.resolve("subtitles.srt");
        subtitleGeneratorService.generateSrt(script, job.getDurationSeconds(), subtitlePath);

        String subtitleUrl = storageClient.upload(
            subtitlePath,
            objectKey(job.getId(), "subtitles.srt"),
            "application/x-subrip"
        );
        return new SubtitleArtifact(subtitlePath, subtitleUrl);
    }

    private String composeAndUploadVideo(VideoJob job, Path audioPath, Path subtitlePath, Path jobDir) {
        Path outputPath = jobDir.resolve("final.mp4");
        Path backgroundPath = resolveBackgroundPath();

        VideoCompositionRequest request = new VideoCompositionRequest(
            backgroundPath,
            audioPath,
            subtitlePath,
            outputPath,
            job.getDurationSeconds(),
            null
        );
        ffmpegVideoComposer.compose(request);

        return storageClient.upload(outputPath, objectKey(job.getId(), "final.mp4"), "video/mp4");
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
            log.info("event=job_step_success jobId={} step={} elapsedMs={}", jobId, step, elapsed);
            return result;
        } catch (Exception ex) {
            long elapsed = System.currentTimeMillis() - startedAt;
            log.error("event=job_step_failed jobId={} step={} elapsedMs={} message={}", jobId, step, elapsed, ex.getMessage());
            throw ex;
        }
    }

    private String buildStepErrorDetails(GenerationStep step, Exception ex) {
        StringBuilder sb = new StringBuilder();
        sb.append("step=").append(step).append('\n');
        sb.append("exception=").append(ex.getClass().getName()).append('\n');
        sb.append("message=").append(ex.getMessage()).append('\n');
        sb.append("stacktrace=").append('\n');
        StackTraceElement[] trace = ex.getStackTrace();
        int maxLines = Math.min(trace.length, 20);
        for (int i = 0; i < maxLines; i++) {
            sb.append("  at ").append(trace[i]).append('\n');
        }
        return sb.toString();
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

    private record AudioArtifact(Path audioPath, String audioUrl, com.autoshorts.ai.entity.AudioGenerationMode mode, String provider) {
    }

    private record SubtitleArtifact(Path subtitlePath, String subtitleUrl) {
    }

    @FunctionalInterface
    private interface StepSupplier<T> {
        T get() throws Exception;
    }
}
