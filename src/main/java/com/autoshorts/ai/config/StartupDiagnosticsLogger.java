package com.autoshorts.ai.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.nio.file.Path;

@Component
public class StartupDiagnosticsLogger implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(StartupDiagnosticsLogger.class);

    private final AppProperties appProperties;

    public StartupDiagnosticsLogger(AppProperties appProperties) {
        this.appProperties = appProperties;
    }

    @Override
    public void run(ApplicationArguments args) {
        String contentProvider = appProperties.getText().getProvider();
        boolean huggingFaceTokenPresent = StringUtils.hasText(appProperties.getHuggingface().getApiToken());
        String contentEffectiveMode = "huggingface".equalsIgnoreCase(contentProvider)
            ? (huggingFaceTokenPresent ? "REAL" : "FALLBACK_ONLY")
            : "UNSUPPORTED";
        boolean visualMockConfigured = appProperties.getVisual().isMock();
        String visualEffectiveMode = !appProperties.getVisual().isEnabled()
            ? "DISABLED"
            : (visualMockConfigured ? "MOCK" : (huggingFaceTokenPresent ? "REAL" : "FALLBACK_ONLY"));
        boolean audioFallbackExpected = appProperties.getElevenlabs().isMock()
            || !"huggingface".equalsIgnoreCase(appProperties.getElevenlabs().getProvider())
            || !StringUtils.hasText(appProperties.getHuggingface().getTtsModel());

        log.info(
            "event=startup_config workingDir={} ffmpegBinary={} contentProvider={} huggingFaceTokenPresent={} contentEffectiveMode={} visualEnabled={} visualProvider={} visualMockConfigured={} visualEffectiveMode={} hfImageModel={} hfVideoModel={} visualMaxScenes={} audioProvider={} hfTtsModelConfigured={} audioFallbackExpected={} storageMock={} storageLocalPublicBaseUrl={} cleanupEnabled={} keepFailedJobFiles={} schedulerEnabled={} schedulerFixedDelayMs={} schedulerBatchSize={} queueEnabled={} queueName={} queueExchange={} queueRoutingKey={} queueDeadLetterName={} queueMaxProcessingAttempts={} publishEnabled={} publishProvider={} publishDefaultPlatform={} webhookEnabled={} webhookEndpointConfigured={}",
            Path.of(appProperties.getWorkingDir()).toAbsolutePath().normalize(),
            appProperties.getFfmpeg().getBinary(),
            contentProvider,
            huggingFaceTokenPresent,
            contentEffectiveMode,
            appProperties.getVisual().isEnabled(),
            appProperties.getVisual().getProvider(),
            visualMockConfigured,
            visualEffectiveMode,
            appProperties.getHuggingface().getImageModel(),
            appProperties.getHuggingface().getVideoModel(),
            appProperties.getVisual().getMaxScenes(),
            appProperties.getElevenlabs().getProvider(),
            StringUtils.hasText(appProperties.getHuggingface().getTtsModel()),
            audioFallbackExpected,
            appProperties.getStorage().isMock(),
            appProperties.getStorage().getLocalPublicBaseUrl(),
            appProperties.getCleanup().isDeleteTempFiles(),
            appProperties.getCleanup().isKeepFailedJobFiles(),
            appProperties.getScheduler().isEnabled(),
            appProperties.getScheduler().getFixedDelayMs(),
            appProperties.getScheduler().getBatchSize(),
            appProperties.getQueue().isEnabled(),
            appProperties.getQueue().getQueue(),
            appProperties.getQueue().getExchange(),
            appProperties.getQueue().getRoutingKey(),
            appProperties.getQueue().getDeadLetterQueue(),
            appProperties.getQueue().getMaxProcessingAttempts(),
            appProperties.getPublish().isEnabled(),
            appProperties.getPublish().getProvider(),
            appProperties.getPublish().getDefaultPlatform(),
            appProperties.getWebhook().isEnabled(),
            StringUtils.hasText(appProperties.getWebhook().getEndpointUrl())
        );
    }
}
