package com.autoshorts.ai.orchestration;

import com.autoshorts.ai.service.TopicAutomationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicBoolean;

@Component
@ConditionalOnProperty(name = "app.scheduler.enabled", havingValue = "true")
public class TopicAutomationScheduler {

    private static final Logger log = LoggerFactory.getLogger(TopicAutomationScheduler.class);

    private final TopicAutomationService topicAutomationService;
    private final AtomicBoolean running = new AtomicBoolean(false);

    public TopicAutomationScheduler(TopicAutomationService topicAutomationService) {
        this.topicAutomationService = topicAutomationService;
    }

    @Scheduled(
        fixedDelayString = "${app.scheduler.fixed-delay-ms:60000}",
        initialDelayString = "${app.scheduler.initial-delay-ms:15000}"
    )
    public void runCycle() {
        if (!running.compareAndSet(false, true)) {
            log.info("event=scheduler_cycle_skipped reason=already_running");
            return;
        }

        long startedAt = System.currentTimeMillis();
        try {
            int dispatched = topicAutomationService.dispatchPendingTopics();
            long elapsed = System.currentTimeMillis() - startedAt;
            log.info("event=scheduler_cycle_completed dispatched={} elapsedMs={}", dispatched, elapsed);
        } catch (Exception ex) {
            long elapsed = System.currentTimeMillis() - startedAt;
            log.error("event=scheduler_cycle_failed elapsedMs={} message={}", elapsed, ex.getMessage(), ex);
        } finally {
            running.set(false);
        }
    }
}