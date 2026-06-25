package com.autoshorts.ai.queue;

import com.autoshorts.ai.entity.GenerationStep;
import com.autoshorts.ai.entity.JobStatus;
import com.autoshorts.ai.entity.NotificationType;
import com.autoshorts.ai.entity.VideoJob;
import com.autoshorts.ai.metrics.PipelineMetrics;
import com.autoshorts.ai.service.NotificationService;
import com.autoshorts.ai.service.VideoJobService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

/**
 * Terminal handler for video jobs that exhausted all processing retries and were dead-lettered.
 * Marks the job FAILED (so it never lingers as PENDING/PROCESSING) and notifies the owner.
 * Never rethrows — a failure here must not bounce the message back into the DLQ.
 */
@Component
@ConditionalOnProperty(name = "app.queue.enabled", havingValue = "true", matchIfMissing = true)
public class VideoJobDeadLetterConsumer {

    private static final Logger log = LoggerFactory.getLogger(VideoJobDeadLetterConsumer.class);

    private final VideoJobService videoJobService;
    private final NotificationService notificationService;
    private final PipelineMetrics pipelineMetrics;

    public VideoJobDeadLetterConsumer(
        VideoJobService videoJobService,
        NotificationService notificationService,
        PipelineMetrics pipelineMetrics
    ) {
        this.videoJobService = videoJobService;
        this.notificationService = notificationService;
        this.pipelineMetrics = pipelineMetrics;
    }

    @RabbitListener(queues = "${app.queue.dead-letter-queue}")
    public void consume(VideoGenerationJobMessage message) {
        pipelineMetrics.recordDeadLettered();
        if (message == null || message.jobId() == null) {
            log.error("event=dlq_message_invalid payload={}", message);
            return;
        }
        UUID jobId = message.jobId();
        log.error("event=job_dead_lettered jobId={} dispatchSource={}", jobId, message.dispatchSource());

        try {
            VideoJob job = videoJobService.getEntityOrThrow(jobId);
            if (job.getStatus() == JobStatus.COMPLETED || job.getStatus() == JobStatus.FAILED) {
                log.info("event=dlq_job_already_terminal jobId={} status={}", jobId, job.getStatus());
                return;
            }
            VideoJob failed = videoJobService.markFailed(
                jobId,
                GenerationStep.QUEUED,
                "Video generation failed after exhausting all retries.",
                "Message was dead-lettered after exhausting queue processing attempts (dispatchSource="
                    + message.dispatchSource() + ")."
            );
            try {
                notificationService.notify(
                    failed.getUserId(),
                    NotificationType.JOB_FAILED,
                    "Tạo video thất bại",
                    "Video cho chủ đề \"" + failed.getTopic() + "\" thất bại sau nhiều lần thử lại.",
                    Map.of("jobId", jobId.toString())
                );
            } catch (Exception notifyEx) {
                log.warn("event=dlq_notify_failed jobId={} message={}", jobId, notifyEx.getMessage());
            }
        } catch (Exception ex) {
            // Swallow: rethrowing would re-dead-letter the message in a loop.
            log.error("event=dlq_handling_error jobId={} message={}", jobId, ex.getMessage(), ex);
        }
    }
}
