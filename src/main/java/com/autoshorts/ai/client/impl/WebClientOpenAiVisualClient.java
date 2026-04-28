package com.autoshorts.ai.client.impl;

import com.autoshorts.ai.client.AiClient;
import com.autoshorts.ai.client.VisualGenerationClient;
import com.autoshorts.ai.client.model.GeneratedVisualImage;
import com.autoshorts.ai.config.AppProperties;
import com.autoshorts.ai.entity.VisualGenerationMode;
import com.autoshorts.ai.exception.ExternalServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.Instant;

@Component
@ConditionalOnProperty(name = "app.visual.mock", havingValue = "false")
public class WebClientOpenAiVisualClient implements VisualGenerationClient {

    private static final Logger log = LoggerFactory.getLogger(WebClientOpenAiVisualClient.class);

    private final AiClient aiClient;
    private final AppProperties appProperties;

    public WebClientOpenAiVisualClient(AiClient aiClient, AppProperties appProperties) {
        this.aiClient = aiClient;
        this.appProperties = appProperties;
    }

    @Override
    public GeneratedVisualImage generateSceneImage(String prompt, int sceneIndex) {
        if (!appProperties.getVisual().isEnabled()) {
            throw new ExternalServiceException("Visual generation disabled");
        }
        if (!StringUtils.hasText(appProperties.getOpenai().getApiKey())) {
            throw new ExternalServiceException("Missing AI API key. Set environment variable: apikey");
        }

        String model = resolveModel();
        String size = resolveSize();
        Instant startedAt = Instant.now();
        AiClient.ImageGenerationResult generated = aiClient.generateImage(prompt, model, size);
        long durationMs = java.time.Duration.between(startedAt, Instant.now()).toMillis();
        byte[] bytes = generated.data();

        if (bytes.length == 0) {
            throw new ExternalServiceException("AI image payload decoded to empty bytes");
        }

        log.info(
            "event=visual_generation_success mode=REAL provider=9router_openai_compatible sceneIndex={} model={} size={} bytes={} durationMs={}",
            sceneIndex,
            generated.model(),
            size,
            bytes.length,
            durationMs
        );

        return new GeneratedVisualImage(
            bytes,
            "png",
            "image/png",
            VisualGenerationMode.REAL,
            "9router_openai_compatible",
            generated.model(),
            durationMs,
            null,
            null
        );
    }

    private String resolveModel() {
        if (StringUtils.hasText(appProperties.getVisual().getModel())) {
            return appProperties.getVisual().getModel().trim();
        }
        return "gpt-image-1";
    }

    private String resolveSize() {
        if (StringUtils.hasText(appProperties.getVisual().getSize())) {
            return appProperties.getVisual().getSize().trim();
        }
        return "1024x1536";
    }
}
