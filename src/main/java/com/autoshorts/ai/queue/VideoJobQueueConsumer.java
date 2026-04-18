package com.autoshorts.ai.queue;

import com.autoshorts.ai.entity.JobStatus;
import com.autoshorts.ai.entity.VideoJob;
import com.autoshorts.ai.orchestration.VideoGenerationOrchestrator;
import com.autoshorts.ai.service.VideoJobService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@ConditionalOnProperty(name = "app.queue.enabled", havingValue = "true", matchIfMissing = true)
public class VideoJobQueueConsumer {

    private static final Logger log = LoggerFactory.getLogger(VideoJobQueueConsumer.class);

    private final VideoGenerationOrchestrator videoGenerationOrchestrator;
    private final VideoJobService videoJobService;

    public VideoJobQueueConsumer(
        VideoGenerationOrchestrator videoGenerationOrchestrator,
        VideoJobService videoJobService
    ) {
        this.videoGenerationOrchestrator = videoGenerationOrchestrator;
        this.videoJobService = videoJobService;
    }

    @RabbitListener(
        queues = "${app.queue.queue}",
        containerFactory = "videoJobRabbitListenerContainerFactory"
    )
    public void consume(
        VideoGenerationJobMessage message,
        @Header(name = AmqpHeaders.RECEIVED_ROUTING_KEY, required = false) String routingKey
    ) {
        if (message == null || message.jobId() == null) {
            throw new IllegalArgumentException("Received empty job message payload");
        }

        UUID jobId = message.jobId();
        log.info(
            "event=job_queue_received jobId={} dispatchSource={} routingKey={} queuedAt={}",
            jobId,
            message.dispatchSource(),
            routingKey,
            message.queuedAt()
        );

        VideoJob job = videoJobService.getEntityOrThrow(jobId);
        if (job.getStatus() != JobStatus.PENDING) {
            log.info(
                "event=job_queue_skipped_non_pending jobId={} status={} dispatchSource={}",
                jobId,
                job.getStatus(),
                message.dispatchSource()
            );
            return;
        }

        videoGenerationOrchestrator.processJob(jobId);
    }
}
