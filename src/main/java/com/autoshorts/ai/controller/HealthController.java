package com.autoshorts.ai.controller;

import com.autoshorts.ai.config.AppProperties;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class HealthController {

    private final AppProperties appProperties;

    public HealthController(AppProperties appProperties) {
        this.appProperties = appProperties;
    }

    @GetMapping("/health")
    public Map<String, Object> health() {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("status", "UP");
        response.put("service", "AutoShorts AI");
        response.put("timestamp", Instant.now());
        response.put("workingDir", Path.of(appProperties.getWorkingDir()).toAbsolutePath().normalize().toString());
        response.put("workingDirWritable", isWorkingDirWritable());
        response.put("ffmpegBinary", appProperties.getFfmpeg().getBinary());
        response.put("contentProvider", appProperties.getText().getProvider());
        response.put("huggingFaceTokenPresent", StringUtils.hasText(appProperties.getHuggingface().getApiToken()));
        response.put("contentEffectiveMode", resolveContentEffectiveMode());
        response.put("visualProvider", appProperties.getVisual().getProvider());
        response.put("hfImageModel", appProperties.getHuggingface().getImageModel());
        response.put("hfVideoModel", appProperties.getHuggingface().getVideoModel());
        response.put("audioProvider", appProperties.getElevenlabs().getProvider());
        response.put("hfTtsModelConfigured", StringUtils.hasText(appProperties.getHuggingface().getTtsModel()));
        response.put("audioFallbackExpected", isAudioFallbackExpected());
        response.put("storageMock", appProperties.getStorage().isMock());
        response.put("schedulerEnabled", appProperties.getScheduler().isEnabled());
        response.put("schedulerBatchSize", appProperties.getScheduler().getBatchSize());
        response.put("queueEnabled", appProperties.getQueue().isEnabled());
        response.put("queueName", appProperties.getQueue().getQueue());
        response.put("queueExchange", appProperties.getQueue().getExchange());
        response.put("queueRoutingKey", appProperties.getQueue().getRoutingKey());
        response.put("queueDeadLetterName", appProperties.getQueue().getDeadLetterQueue());
        response.put("queueMaxProcessingAttempts", appProperties.getQueue().getMaxProcessingAttempts());
        return response;
    }

    private boolean isWorkingDirWritable() {
        try {
            Path workingDir = Path.of(appProperties.getWorkingDir()).toAbsolutePath().normalize();
            Files.createDirectories(workingDir);
            return Files.isWritable(workingDir);
        } catch (Exception ex) {
            return false;
        }
    }

    private String resolveContentEffectiveMode() {
        if ("huggingface".equalsIgnoreCase(appProperties.getText().getProvider())
            && StringUtils.hasText(appProperties.getHuggingface().getApiToken())) {
            return "REAL";
        }
        return "FALLBACK_ONLY";
    }

    private boolean isAudioFallbackExpected() {
        return appProperties.getElevenlabs().isMock()
            || !"huggingface".equalsIgnoreCase(appProperties.getElevenlabs().getProvider())
            || !StringUtils.hasText(appProperties.getHuggingface().getTtsModel());
    }
}
