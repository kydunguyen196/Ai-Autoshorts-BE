package com.autoshorts.ai.queue;

import com.autoshorts.ai.config.AppProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Service
public class VideoJobQueuePublisher {

    private static final Logger log = LoggerFactory.getLogger(VideoJobQueuePublisher.class);

    private final RabbitTemplate rabbitTemplate;
    private final AppProperties appProperties;

    public VideoJobQueuePublisher(RabbitTemplate rabbitTemplate, AppProperties appProperties) {
        this.rabbitTemplate = rabbitTemplate;
        this.appProperties = appProperties;
    }

    public void publish(UUID jobId, String dispatchSource) {
        if (!appProperties.getQueue().isEnabled()) {
            throw new IllegalStateException("Queue dispatch is disabled (app.queue.enabled=false)");
        }
        VideoGenerationJobMessage message = new VideoGenerationJobMessage(
            jobId,
            dispatchSource,
            Instant.now()
        );
        String exchange = appProperties.getQueue().getExchange();
        String routingKey = appProperties.getQueue().getRoutingKey();

        rabbitTemplate.convertAndSend(exchange, routingKey, message);
        log.info(
            "event=job_queue_published jobId={} dispatchSource={} exchange={} routingKey={}",
            jobId,
            dispatchSource,
            exchange,
            routingKey
        );
    }
}
