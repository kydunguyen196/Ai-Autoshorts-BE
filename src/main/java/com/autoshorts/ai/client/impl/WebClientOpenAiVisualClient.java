package com.autoshorts.ai.client.impl;

import com.autoshorts.ai.client.VisualGenerationClient;
import com.autoshorts.ai.client.model.GeneratedVisualImage;
import com.autoshorts.ai.config.AppProperties;
import com.autoshorts.ai.entity.VisualGenerationMode;
import com.autoshorts.ai.exception.ExternalServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;
import java.net.URI;
import java.util.Base64;
import java.util.List;
import java.util.Map;

@Component
@ConditionalOnProperty(name = "app.visual.mock", havingValue = "false")
public class WebClientOpenAiVisualClient implements VisualGenerationClient {

    private static final Logger log = LoggerFactory.getLogger(WebClientOpenAiVisualClient.class);

    private final WebClient webClient;
    private final AppProperties appProperties;

    public WebClientOpenAiVisualClient(WebClient.Builder webClientBuilder, AppProperties appProperties) {
        this.appProperties = appProperties;
        this.webClient = webClientBuilder
            .baseUrl(appProperties.getOpenai().getBaseUrl())
            .build();
    }

    @Override
    public GeneratedVisualImage generateSceneImage(String prompt, int sceneIndex) {
        if (!appProperties.getVisual().isEnabled()) {
            throw new ExternalServiceException("Visual generation disabled");
        }
        if (!StringUtils.hasText(appProperties.getOpenai().getApiKey())) {
            throw new ExternalServiceException("OPENAI_API_KEY is missing for visual generation");
        }

        String model = resolveModel();
        String size = resolveSize();
        Instant startedAt = Instant.now();

        Map<String, Object> requestBody = Map.of(
            "model", model,
            "prompt", prompt,
            "size", size
        );

        OpenAiImageResponse response = webClient.post()
            .uri("/v1/images/generations")
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + appProperties.getOpenai().getApiKey())
            .contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON)
            .bodyValue(requestBody)
            .retrieve()
            .onStatus(HttpStatusCode::isError, clientResponse ->
                clientResponse.bodyToMono(String.class)
                    .defaultIfEmpty("")
                    .flatMap(body -> Mono.error(new ExternalServiceException(
                        "OpenAI image request failed: status=%s body=%s".formatted(clientResponse.statusCode(), truncate(body, 400))
                    )))
            )
            .bodyToMono(OpenAiImageResponse.class)
            .block(Duration.ofMillis(Math.max(1000, appProperties.getVisual().getRequestTimeoutMs())));

        long durationMs = Duration.between(startedAt, Instant.now()).toMillis();
        if (response == null || response.getData() == null || response.getData().isEmpty()) {
            throw new ExternalServiceException("OpenAI image response is empty");
        }

        ImageData firstImage = response.getData().get(0);
        byte[] bytes = decodeImagePayload(firstImage);

        if (bytes.length == 0) {
            throw new ExternalServiceException("OpenAI image payload decoded to empty bytes");
        }

        log.info(
            "event=visual_generation_success mode=REAL provider=openai sceneIndex={} model={} size={} bytes={} durationMs={}",
            sceneIndex,
            model,
            size,
            bytes.length,
            durationMs
        );

        return new GeneratedVisualImage(
            bytes,
            "png",
            "image/png",
            VisualGenerationMode.REAL,
            "openai_images",
            model,
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

    private String truncate(String value, int maxLength) {
        if (value == null) {
            return "";
        }
        if (value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }

    private byte[] decodeImagePayload(ImageData imageData) {
        String b64 = imageData.getB64Json();
        if (StringUtils.hasText(b64)) {
            try {
                return Base64.getDecoder().decode(b64);
            } catch (IllegalArgumentException ex) {
                throw new ExternalServiceException("OpenAI image response contains invalid base64 payload");
            }
        }

        String url = imageData.getUrl();
        if (!StringUtils.hasText(url)) {
            throw new ExternalServiceException("OpenAI image response missing both b64_json and url");
        }

        try {
            URI imageUri = URI.create(url);
            byte[] downloaded = webClient.get()
                .uri(imageUri)
                .accept(MediaType.APPLICATION_OCTET_STREAM)
                .retrieve()
                .onStatus(HttpStatusCode::isError, clientResponse ->
                    clientResponse.bodyToMono(String.class)
                        .defaultIfEmpty("")
                        .flatMap(body -> Mono.error(new ExternalServiceException(
                            "OpenAI image download failed: status=%s body=%s"
                                .formatted(clientResponse.statusCode(), truncate(body, 400))
                        )))
                )
                .bodyToMono(byte[].class)
                .block(Duration.ofMillis(Math.max(1000, appProperties.getVisual().getRequestTimeoutMs())));
            if (downloaded == null || downloaded.length == 0) {
                throw new ExternalServiceException("OpenAI image url download returned empty payload");
            }
            return downloaded;
        } catch (IllegalArgumentException ex) {
            throw new ExternalServiceException("OpenAI image response contains invalid url");
        }
    }

    private static class OpenAiImageResponse {
        private List<ImageData> data;

        public List<ImageData> getData() {
            return data;
        }

        public void setData(List<ImageData> data) {
            this.data = data;
        }
    }

    private static class ImageData {
        private String b64_json;
        private String b64Json;
        private String url;

        public String getB64_json() {
            return b64_json;
        }

        public void setB64_json(String b64_json) {
            this.b64_json = b64_json;
        }

        public String getB64Json() {
            if (StringUtils.hasText(b64Json)) {
                return b64Json;
            }
            return b64_json;
        }

        public void setB64Json(String b64Json) {
            this.b64Json = b64Json;
        }

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }
    }
}
