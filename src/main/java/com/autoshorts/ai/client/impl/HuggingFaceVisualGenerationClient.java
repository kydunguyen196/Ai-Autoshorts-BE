package com.autoshorts.ai.client.impl;

import com.autoshorts.ai.client.VisualGenerationClient;
import com.autoshorts.ai.client.model.GeneratedVisualImage;
import com.autoshorts.ai.config.AppProperties;
import com.autoshorts.ai.entity.VisualGenerationMode;
import com.autoshorts.ai.exception.ExternalServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
@ConditionalOnExpression("'${app.visual.mock:true}' == 'false' && '${app.visual.provider:openai}' == 'huggingface'")
public class HuggingFaceVisualGenerationClient implements VisualGenerationClient {

    private static final Logger log = LoggerFactory.getLogger(HuggingFaceVisualGenerationClient.class);

    private final AppProperties appProperties;
    private final WebClient.Builder webClientBuilder;

    public HuggingFaceVisualGenerationClient(AppProperties appProperties, WebClient.Builder webClientBuilder) {
        this.appProperties = appProperties;
        this.webClientBuilder = webClientBuilder;
    }

    @Override
    public GeneratedVisualImage generateSceneImage(String prompt, int sceneIndex) {
        if (!appProperties.getVisual().isEnabled()) {
            throw new ExternalServiceException("Visual generation disabled");
        }
        String model = resolveModel();
        String provider = resolveProvider();
        WebClient client = HuggingFaceSupport.webClient(webClientBuilder, appProperties);
        Instant startedAt = Instant.now();

        Map<String, Object> parameters = new LinkedHashMap<>();
        Size size = parseSize(appProperties.getVisual().getSize());
        parameters.put("width", size.width());
        parameters.put("height", size.height());
        parameters.put("num_inference_steps", 4);
        parameters.put(
            "negative_prompt",
            "text, letters, words, captions, subtitles, logo, watermark, UI, poster typography, misspelled text, distorted hands, extra fingers, low quality"
        );

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("inputs", prompt);
        body.put("parameters", parameters);

        byte[] bytes = client
            .post()
            .uri(HuggingFaceSupport.providerModelPath(provider, model))
            .header(HttpHeaders.ACCEPT, "image/png")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(body)
            .retrieve()
            .onStatus(HuggingFaceSupport::isError, HuggingFaceSupport::errorFromResponse)
            .bodyToMono(byte[].class)
            .block(Duration.ofMillis(Math.max(1000L, appProperties.getHuggingface().getRequestTimeoutMs())));

        if (bytes == null || bytes.length == 0) {
            throw new ExternalServiceException("Hugging Face image response is invalid: empty image payload");
        }
        long durationMs = Duration.between(startedAt, Instant.now()).toMillis();
        log.info(
            "event=huggingface_visual_generation_success provider={} sceneIndex={} model={} bytes={} durationMs={}",
            provider,
            sceneIndex,
            model,
            bytes.length,
            durationMs
        );
        return new GeneratedVisualImage(
            bytes,
            "png",
            "image/png",
            VisualGenerationMode.REAL,
            "huggingface:" + provider,
            model,
            durationMs,
            null,
            null
        );
    }

    private String resolveModel() {
        String model = HuggingFaceSupport.trimToNull(appProperties.getHuggingface().getImageModel());
        if (!StringUtils.hasText(model)) {
            model = HuggingFaceSupport.trimToNull(appProperties.getVisual().getModel());
        }
        if (!StringUtils.hasText(model)) {
            throw new ExternalServiceException("Missing HF_IMAGE_MODEL");
        }
        return model;
    }

    private String resolveProvider() {
        String provider = HuggingFaceSupport.trimToNull(appProperties.getHuggingface().getImageProvider());
        return StringUtils.hasText(provider) ? provider : "hf-inference";
    }

    private Size parseSize(String rawSize) {
        if (!StringUtils.hasText(rawSize) || !rawSize.contains("x")) {
            return new Size(1024, 1536);
        }
        String[] parts = rawSize.toLowerCase().split("x", 2);
        try {
            return new Size(Integer.parseInt(parts[0].trim()), Integer.parseInt(parts[1].trim()));
        } catch (NumberFormatException ex) {
            return new Size(1024, 1536);
        }
    }

    private record Size(int width, int height) {
    }
}
