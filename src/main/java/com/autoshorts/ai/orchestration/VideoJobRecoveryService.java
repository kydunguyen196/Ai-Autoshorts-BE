package com.autoshorts.ai.orchestration;

import com.autoshorts.ai.config.AppProperties;
import com.autoshorts.ai.entity.GenerationStep;
import com.autoshorts.ai.entity.JobStatus;
import com.autoshorts.ai.entity.VideoJob;
import com.autoshorts.ai.repository.VideoJobRepository;
import com.autoshorts.ai.service.VideoJobDispatchService;
import com.autoshorts.ai.service.VideoJobService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Service
public class VideoJobRecoveryService {

    private static final Logger log = LoggerFactory.getLogger(VideoJobRecoveryService.class);

    private static final String RECOVERY_DISPATCH_SOURCE = "startup_recovery";
    private static final String ORPHANED_PENDING_FAILURE_CONTEXT = "Startup recovery failed to redispatch orphaned pending job";

    private final VideoJobRepository videoJobRepository;
    private final VideoJobService videoJobService;
    private final VideoJobDispatchService videoJobDispatchService;
    private final AppProperties appProperties;

    public VideoJobRecoveryService(
        VideoJobRepository videoJobRepository,
        VideoJobService videoJobService,
        VideoJobDispatchService videoJobDispatchService,
        AppProperties appProperties
    ) {
        this.videoJobRepository = videoJobRepository;
        this.videoJobService = videoJobService;
        this.videoJobDispatchService = videoJobDispatchService;
        this.appProperties = appProperties;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void recoverJobsOnStartup() {
        if (!appProperties.getQueue().isEnabled() || !appProperties.getQueue().isRecoveryEnabled()) {
            log.info(
                "event=job_recovery_skipped queueEnabled={} recoveryEnabled={}",
                appProperties.getQueue().isEnabled(),
                appProperties.getQueue().isRecoveryEnabled()
            );
            return;
        }

        RecoverySummary summary = recoverOrphanedJobs(Instant.now());
        log.info(
            "event=job_recovery_completed staleProcessingFailed={} pendingRedispatched={} pendingRedispatchFailed={}",
            summary.staleProcessingFailed(),
            summary.pendingRedispatched(),
            summary.pendingRedispatchFailed()
        );
    }

    public RecoverySummary recoverOrphanedJobs(Instant now) {
        int staleProcessingFailed = failStaleProcessingJobs(now);
        PendingRedispatchSummary pendingSummary = redispatchOrphanedPendingJobs(now);
        return new RecoverySummary(
            staleProcessingFailed,
            pendingSummary.redispatched(),
            pendingSummary.failed()
        );
    }

    private int failStaleProcessingJobs(Instant now) {
        Instant cutoff = now.minus(Duration.ofMinutes(appProperties.getQueue().getStuckProcessingTimeoutMinutes()));
        List<VideoJob> staleJobs = videoJobRepository.findByStatusAndUpdatedAtBefore(
            JobStatus.PROCESSING,
            cutoff,
            PageRequest.of(0, appProperties.getQueue().getRecoveryBatchSize())
        );

        int failed = 0;
        for (VideoJob job : staleJobs) {
            try {
                videoJobService.markFailed(
                    job.getId(),
                    job.getCurrentStep() == null ? GenerationStep.QUEUED : job.getCurrentStep(),
                    "Job was still PROCESSING after application restart recovery window",
                    "step=" + (job.getCurrentStep() == null ? GenerationStep.QUEUED : job.getCurrentStep())
                        + " status=PROCESSING"
                        + " updatedAt=" + job.getUpdatedAt()
                        + " recoveryCutoff=" + cutoff
                );
                failed++;
                log.warn(
                    "event=stale_processing_job_failed jobId={} currentStep={} updatedAt={} cutoff={}",
                    job.getId(),
                    job.getCurrentStep(),
                    job.getUpdatedAt(),
                    cutoff
                );
            } catch (Exception ex) {
                log.error(
                    "event=stale_processing_job_recovery_failed jobId={} message={}",
                    job.getId(),
                    ex.getMessage(),
                    ex
                );
            }
        }
        return failed;
    }

    private PendingRedispatchSummary redispatchOrphanedPendingJobs(Instant now) {
        Instant cutoff = now.minus(Duration.ofMinutes(appProperties.getQueue().getPendingRedispatchDelayMinutes()));
        List<VideoJob> pendingJobs = videoJobRepository.findByStatusAndCreatedAtBefore(
            JobStatus.PENDING,
            cutoff,
            PageRequest.of(0, appProperties.getQueue().getRecoveryBatchSize())
        );

        int redispatched = 0;
        int failed = 0;
        for (VideoJob job : pendingJobs) {
            try {
                videoJobDispatchService.publishOrMarkFailed(
                    job.getId(),
                    RECOVERY_DISPATCH_SOURCE,
                    ORPHANED_PENDING_FAILURE_CONTEXT
                );
                redispatched++;
                log.info(
                    "event=orphaned_pending_job_redispatched jobId={} createdAt={} cutoff={}",
                    job.getId(),
                    job.getCreatedAt(),
                    cutoff
                );
            } catch (Exception ex) {
                failed++;
                log.error(
                    "event=orphaned_pending_job_redispatch_failed jobId={} message={}",
                    job.getId(),
                    ex.getMessage(),
                    ex
                );
            }
        }
        return new PendingRedispatchSummary(redispatched, failed);
    }

    public record RecoverySummary(int staleProcessingFailed, int pendingRedispatched, int pendingRedispatchFailed) {
    }

    private record PendingRedispatchSummary(int redispatched, int failed) {
    }
}
