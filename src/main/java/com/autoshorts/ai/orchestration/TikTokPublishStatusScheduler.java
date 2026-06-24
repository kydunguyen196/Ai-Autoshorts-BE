package com.autoshorts.ai.orchestration;

import com.autoshorts.ai.service.TikTokPublishStatusService;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Periodically polls TikTok for the completion status of submitted posts. Only active when
 * {@code app.tiktok.direct-publish-enabled=true}; the mock/scaffold path needs no reconciliation.
 */
@Component
@ConditionalOnProperty(name = "app.tiktok.direct-publish-enabled", havingValue = "true")
public class TikTokPublishStatusScheduler {

    private static final Logger log = LoggerFactory.getLogger(TikTokPublishStatusScheduler.class);

    private final TikTokPublishStatusService tikTokPublishStatusService;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private volatile boolean shuttingDown;

    public TikTokPublishStatusScheduler(TikTokPublishStatusService tikTokPublishStatusService) {
        this.tikTokPublishStatusService = tikTokPublishStatusService;
    }

    @Scheduled(
        fixedDelayString = "${app.tiktok.status-poll-fixed-delay-ms:30000}",
        initialDelayString = "${app.tiktok.status-poll-fixed-delay-ms:30000}"
    )
    public void pollPendingPublishes() {
        if (shuttingDown) {
            return;
        }
        if (!running.compareAndSet(false, true)) {
            return;
        }
        long startedAt = System.currentTimeMillis();
        try {
            int finalized = tikTokPublishStatusService.reconcilePendingPublishes();
            if (finalized > 0) {
                log.info("event=tiktok_status_poll_cycle finalized={} elapsedMs={}", finalized, System.currentTimeMillis() - startedAt);
            }
        } catch (Exception ex) {
            log.error("event=tiktok_status_poll_cycle_failed elapsedMs={} message={}", System.currentTimeMillis() - startedAt, ex.getMessage(), ex);
        } finally {
            running.set(false);
        }
    }

    @PreDestroy
    public void onShutdown() {
        shuttingDown = true;
    }

    @EventListener(ContextClosedEvent.class)
    public void onContextClosed() {
        shuttingDown = true;
    }
}
