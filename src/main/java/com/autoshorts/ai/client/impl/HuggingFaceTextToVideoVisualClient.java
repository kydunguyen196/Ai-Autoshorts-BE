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
import reactor.util.retry.Retry;

import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Component
@ConditionalOnExpression("'${app.visual.mock:true}' == 'false' && '${app.visual.provider:openai}' == 'huggingface-video'")
public class HuggingFaceTextToVideoVisualClient implements VisualGenerationClient {

    private static final Logger log = LoggerFactory.getLogger(HuggingFaceTextToVideoVisualClient.class);
    private static final Duration POLL_INTERVAL = Duration.ofSeconds(2);

    private final AppProperties appProperties;
    private final WebClient.Builder webClientBuilder;

    public HuggingFaceTextToVideoVisualClient(AppProperties appProperties, WebClient.Builder webClientBuilder) {
        this.appProperties = appProperties;
        this.webClientBuilder = webClientBuilder;
    }

    @Override
    public GeneratedVisualImage generateSceneImage(String prompt, int sceneIndex) {
        if (!appProperties.getVisual().isEnabled()) {
            throw new ExternalServiceException("Visual generation disabled");
        }

        String provider = resolveProvider();
        String model = resolveModel();
        WebClient client = HuggingFaceSupport.webClient(webClientBuilder, appProperties);
        Instant startedAt = Instant.now();

        byte[] bytes = generateFalAiVideo(client, prompt, provider, model);

        if (bytes == null || bytes.length == 0) {
            throw new ExternalServiceException("Hugging Face text-to-video response is invalid: empty video payload");
        }

        long durationMs = Duration.between(startedAt, Instant.now()).toMillis();
        log.info(
            "event=huggingface_text_to_video_success provider={} sceneIndex={} model={} bytes={} durationMs={}",
            provider,
            sceneIndex,
            model,
            bytes.length,
            durationMs
        );

        return new GeneratedVisualImage(
            bytes,
            "mp4",
            "video/mp4",
            VisualGenerationMode.REAL,
            "huggingface-video:" + provider,
            model,
            durationMs,
            null,
            null
        );
    }

    private byte[] generateFalAiVideo(WebClient client, String prompt, String provider, String model) {
        if (!"fal-ai".equalsIgnoreCase(provider)) {
            throw new ExternalServiceException("Hugging Face text-to-video currently supports HF-routed fal-ai only. Set HF_VIDEO_PROVIDER=fal-ai");
        }

        String providerModelId = resolveProviderModelId(client, provider, model);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("prompt", prompt);
        body.put("num_frames", 49);
        body.put("num_inference_steps", 24);
        body.put("guidance_scale", 6.0);
        body.put("negative_prompt", List.of("text", "letters", "watermark", "logo", "subtitles", "low quality", "distorted hands"));

        Map<String, Object> submitResponse = postJson(
            client,
            "/fal-ai/" + providerModelId + "?_subdomain=queue",
            body
        );

        Map<String, Object> completed = waitForFalAiCompletion(client, submitResponse);
        String videoUrl = extractVideoUrl(completed);
        return downloadVideo(videoUrl);
    }

    @SuppressWarnings("unchecked")
    private String resolveProviderModelId(WebClient client, String provider, String model) {
        Map<String, Object> modelInfo = client
            .get()
            .uri("https://huggingface.co/api/models/" + model + "?expand=inferenceProviderMapping")
            .retrieve()
            .onStatus(HuggingFaceSupport::isError, HuggingFaceSupport::errorFromResponse)
            .bodyToMono(Map.class)
            .block(timeout());

        Object mappings = modelInfo == null ? null : modelInfo.get("inferenceProviderMapping");
        if (mappings instanceof Map<?, ?> mappingMap) {
            Object providerMapping = mappingMap.get(provider);
            if (providerMapping instanceof Map<?, ?> providerMap) {
                Object providerId = providerMap.get("providerId");
                if (providerId instanceof String value && StringUtils.hasText(value)) {
                    return value;
                }
            }
        }

        if ("Wan-AI/Wan2.2-T2V-A14B".equals(model)) {
            return "fal-ai/wan/v2.2-a14b/text-to-video";
        }
        throw new ExternalServiceException("No Hugging Face inference provider mapping found for text-to-video model: " + model);
    }

    private Map<String, Object> waitForFalAiCompletion(WebClient client, Map<String, Object> submitResponse) {
        String status = stringValue(submitResponse.get("status"));
        String responseUrl = stringValue(submitResponse.get("response_url"));
        String statusUrl = toHfFalQueuePath(stringValue(submitResponse.get("status_url")));
        String resultUrl = toHfFalQueuePath(responseUrl);
        long deadline = System.nanoTime() + timeout().toNanos();

        while (!"COMPLETED".equalsIgnoreCase(status)) {
            if ("FAILED".equalsIgnoreCase(status) || "ERROR".equalsIgnoreCase(status)) {
                throw new ExternalServiceException("Hugging Face fal-ai text-to-video failed with status=" + status);
            }
            if (System.nanoTime() > deadline) {
                throw new ExternalServiceException("Hugging Face fal-ai text-to-video timed out while waiting for queue completion");
            }
            sleep(POLL_INTERVAL);
            String pollPath = StringUtils.hasText(statusUrl)
                ? statusUrl
                : resultUrl.replace("?_subdomain=queue", "/status?_subdomain=queue");
            Map<String, Object> statusResponse = getJson(client, pollPath);
            status = stringValue(statusResponse.get("status"));
        }

        return getJson(client, resultUrl);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> postJson(WebClient client, String path, Map<String, Object> body) {
        return client
            .post()
            .uri(path)
            .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(body)
            .retrieve()
            .onStatus(HuggingFaceSupport::isError, HuggingFaceSupport::errorFromResponse)
            .bodyToMono(Map.class)
            .retryWhen(Retry.backoff(1, Duration.ofSeconds(2)).filter(this::isTransientFailure))
            .block(timeout());
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> getJson(WebClient client, String pathOrUrl) {
        return client
            .get()
            .uri(pathOrUrl)
            .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
            .retrieve()
            .onStatus(HuggingFaceSupport::isError, HuggingFaceSupport::errorFromResponse)
            .bodyToMono(Map.class)
            .block(timeout());
    }

    @SuppressWarnings("unchecked")
    private String extractVideoUrl(Map<String, Object> completed) {
        Object video = completed == null ? null : completed.get("video");
        if (video instanceof Map<?, ?> videoMap) {
            Object url = videoMap.get("url");
            if (url instanceof String value && StringUtils.hasText(value)) {
                return value;
            }
        }
        throw new ExternalServiceException("Hugging Face fal-ai text-to-video result does not include video.url");
    }

    private byte[] downloadVideo(String videoUrl) {
        return webClientBuilder
            .build()
            .get()
            .uri(videoUrl)
            .header(HttpHeaders.ACCEPT, "video/mp4, application/octet-stream")
            .retrieve()
            .onStatus(HuggingFaceSupport::isError, HuggingFaceSupport::errorFromResponse)
            .bodyToMono(byte[].class)
            .block(timeout());
    }

    private Duration timeout() {
        return Duration.ofMillis(Math.max(1000L, appProperties.getHuggingface().getRequestTimeoutMs()));
    }

    private String toHfFalQueuePath(String value) {
        if (!StringUtils.hasText(value)) {
            return "";
        }
        if (!value.startsWith("http://") && !value.startsWith("https://")) {
            return appendQueueSubdomain(value);
        }
        URI uri = URI.create(value);
        String path = uri.getPath();
        if ("router.huggingface.co".equalsIgnoreCase(uri.getHost())) {
            return appendQueueSubdomain(path);
        }
        return appendQueueSubdomain("/fal-ai" + path);
    }

    private String appendQueueSubdomain(String path) {
        if (!StringUtils.hasText(path) || path.contains("_subdomain=queue")) {
            return path;
        }
        return path + (path.contains("?") ? "&" : "?") + "_subdomain=queue";
    }

    private String stringValue(Object value) {
        return Objects.toString(value, null);
    }

    private void sleep(Duration duration) {
        try {
            Thread.sleep(duration.toMillis());
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new ExternalServiceException("Interrupted while waiting for Hugging Face fal-ai text-to-video result", ex);
        }
    }

    private String resolveProvider() {
        String provider = HuggingFaceSupport.trimToNull(appProperties.getHuggingface().getVideoProvider());
        return StringUtils.hasText(provider) ? provider : "fal-ai";
    }

    private String resolveModel() {
        String model = HuggingFaceSupport.trimToNull(appProperties.getHuggingface().getVideoModel());
        if (!StringUtils.hasText(model)) {
            throw new ExternalServiceException("Missing HF_VIDEO_MODEL");
        }
        return model;
    }

    private boolean isTransientFailure(Throwable throwable) {
        String message = throwable == null ? "" : throwable.getMessage();
        return message.contains("Failed to resolve")
            || message.contains("Connection reset")
            || message.contains("Connection timed out")
            || message.contains("PrematureCloseException")
            || message.contains("503");
    }
}
