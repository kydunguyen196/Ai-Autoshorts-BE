package com.autoshorts.ai.orchestration;

import com.autoshorts.ai.service.SocialPublishStatusService;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Periodically polls YouTube / Instagram for the completion status of submitted posts. Active only
 * when at least one of those platforms has direct publish enabled; the scaffold path needs no
 * reconciliation.
 */
@Component
@ConditionalOnExpression(
    "${app.youtube.direct-publish-enabled:false} or ${app.instagram.direct-publish-enabled:false}")
public class SocialPublishStatusScheduler {

    private static final Logger log = LoggerFactory.getLogger(SocialPublishStatusScheduler.class);

    private final SocialPublishStatusService socialPublishStatusService;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private volatile boolean shuttingDown;

    public SocialPublishStatusScheduler(SocialPublishStatusService socialPublishStatusService) {
        this.socialPublishStatusService = socialPublishStatusService;
    }

    @Scheduled(
        fixedDelayString = "${app.youtube.status-poll-fixed-delay-ms:30000}",
        initialDelayString = "${app.youtube.status-poll-fixed-delay-ms:30000}"
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
            int finalized = socialPublishStatusService.reconcilePendingPublishes();
            if (finalized > 0) {
                log.info("event=social_status_poll_cycle finalized={} elapsedMs={}",
                    finalized, System.currentTimeMillis() - startedAt);
            }
        } catch (Exception ex) {
            log.error("event=social_status_poll_cycle_failed elapsedMs={} message={}",
                System.currentTimeMillis() - startedAt, ex.getMessage(), ex);
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
