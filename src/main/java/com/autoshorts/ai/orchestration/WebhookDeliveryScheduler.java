package com.autoshorts.ai.orchestration;

import com.autoshorts.ai.service.WebhookDeliveryService;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicBoolean;

@Component
@ConditionalOnProperty(name = "app.webhook.enabled", havingValue = "true")
public class WebhookDeliveryScheduler {

    private static final Logger log = LoggerFactory.getLogger(WebhookDeliveryScheduler.class);

    private final WebhookDeliveryService webhookDeliveryService;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private volatile boolean shuttingDown;

    public WebhookDeliveryScheduler(WebhookDeliveryService webhookDeliveryService) {
        this.webhookDeliveryService = webhookDeliveryService;
    }

    @Scheduled(
        fixedDelayString = "${app.webhook.dispatch-fixed-delay-ms:5000}",
        initialDelayString = "${app.webhook.dispatch-fixed-delay-ms:5000}"
    )
    public void dispatchDueEvents() {
        if (shuttingDown) {
            return;
        }
        if (!running.compareAndSet(false, true)) {
            return;
        }

        long startedAt = System.currentTimeMillis();
        try {
            int processed = webhookDeliveryService.dispatchDueDeliveries();
            long elapsed = System.currentTimeMillis() - startedAt;
            if (processed > 0) {
                log.info("event=webhook_dispatch_cycle processed={} elapsedMs={}", processed, elapsed);
            }
        } catch (Exception ex) {
            long elapsed = System.currentTimeMillis() - startedAt;
            log.error("event=webhook_dispatch_cycle_failed elapsedMs={} message={}", elapsed, ex.getMessage(), ex);
        } finally {
            running.set(false);
        }
    }

    @PreDestroy
    public void onShutdown() {
        shuttingDown = true;
    }
}
