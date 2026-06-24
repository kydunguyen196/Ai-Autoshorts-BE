package com.autoshorts.ai.orchestration;

import com.autoshorts.ai.config.AppProperties;
import com.autoshorts.ai.service.SettingsService;
import com.autoshorts.ai.service.TopicAutomationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Drives topic-to-video automation. The bean always exists; whether a cycle does work
 * is decided at runtime by {@code scheduler.enabled} (overridable by admins via
 * {@link SettingsService}, falling back to {@code app.scheduler.enabled} from config).
 */
@Component
public class TopicAutomationScheduler {

    public static final String SETTING_ENABLED = "scheduler.enabled";

    private static final Logger log = LoggerFactory.getLogger(TopicAutomationScheduler.class);

    private final TopicAutomationService topicAutomationService;
    private final SettingsService settingsService;
    private final AppProperties appProperties;
    private final AtomicBoolean running = new AtomicBoolean(false);

    public TopicAutomationScheduler(
        TopicAutomationService topicAutomationService,
        SettingsService settingsService,
        AppProperties appProperties
    ) {
        this.topicAutomationService = topicAutomationService;
        this.settingsService = settingsService;
        this.appProperties = appProperties;
    }

    @Scheduled(
        fixedDelayString = "${app.scheduler.fixed-delay-ms:60000}",
        initialDelayString = "${app.scheduler.initial-delay-ms:15000}"
    )
    public void runCycle() {
        if (!settingsService.getBoolean(SETTING_ENABLED, appProperties.getScheduler().isEnabled())) {
            return;
        }
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
