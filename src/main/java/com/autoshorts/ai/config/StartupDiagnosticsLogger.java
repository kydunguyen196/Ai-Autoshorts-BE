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
        boolean openAiMockConfigured = appProperties.getOpenai().isMock();
        boolean openAiApiKeyPresent = StringUtils.hasText(appProperties.getOpenai().getApiKey());
        String openAiEffectiveMode = openAiMockConfigured
            ? "MOCK"
            : (openAiApiKeyPresent ? "REAL" : "FALLBACK_ONLY");
        boolean elevenLabsMockEffective = appProperties.getElevenlabs().isMock()
            || !StringUtils.hasText(appProperties.getElevenlabs().getApiKey());

        log.info(
            "event=startup_config workingDir={} ffmpegBinary={} openAiMockConfigured={} openAiApiKeyPresent={} openAiEffectiveMode={} elevenLabsMockEffective={} storageMock={} storageLocalPublicBaseUrl={} cleanupEnabled={} keepFailedJobFiles={} schedulerEnabled={} schedulerFixedDelayMs={} schedulerBatchSize={} queueEnabled={} queueName={} queueExchange={} queueRoutingKey={} queueDeadLetterName={} queueMaxProcessingAttempts={} publishEnabled={} publishProvider={} publishDefaultPlatform={} webhookEnabled={} webhookEndpointConfigured={}",
            Path.of(appProperties.getWorkingDir()).toAbsolutePath().normalize(),
            appProperties.getFfmpeg().getBinary(),
            openAiMockConfigured,
            openAiApiKeyPresent,
            openAiEffectiveMode,
            elevenLabsMockEffective,
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
