package com.autoshorts.ai.orchestration;

import com.autoshorts.ai.config.AppProperties;
import com.autoshorts.ai.service.NewsIngestionService;
import com.autoshorts.ai.service.SettingsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Periodically pulls due RSS sources. Enablement is decided at runtime via
 * {@code news.enabled} ({@link SettingsService}, falling back to {@code app.news.enabled}),
 * so admins can toggle ingestion without a restart.
 */
@Component
public class NewsIngestionScheduler {

    public static final String SETTING_ENABLED = "news.enabled";

    private static final Logger log = LoggerFactory.getLogger(NewsIngestionScheduler.class);

    private final NewsIngestionService newsIngestionService;
    private final SettingsService settingsService;
    private final AppProperties appProperties;
    private final AtomicBoolean running = new AtomicBoolean(false);

    public NewsIngestionScheduler(
        NewsIngestionService newsIngestionService,
        SettingsService settingsService,
        AppProperties appProperties
    ) {
        this.newsIngestionService = newsIngestionService;
        this.settingsService = settingsService;
        this.appProperties = appProperties;
    }

    @Scheduled(
        fixedDelayString = "${app.news.fixed-delay-ms:300000}",
        initialDelayString = "${app.news.initial-delay-ms:30000}"
    )
    public void runCycle() {
        if (!settingsService.getBoolean(SETTING_ENABLED, appProperties.getNews().isEnabled())) {
            return;
        }
        if (!running.compareAndSet(false, true)) {
            log.info("event=news_cycle_skipped reason=already_running");
            return;
        }
        long startedAt = System.currentTimeMillis();
        try {
            int ingested = newsIngestionService.ingestDueSources();
            log.info("event=news_cycle_completed ingested={} elapsedMs={}", ingested, System.currentTimeMillis() - startedAt);
        } catch (Exception ex) {
            log.error("event=news_cycle_failed message={}", ex.getMessage(), ex);
        } finally {
            running.set(false);
        }
    }
}
