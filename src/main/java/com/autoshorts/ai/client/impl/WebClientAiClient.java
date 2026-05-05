package com.autoshorts.ai.client.impl;

import com.autoshorts.ai.client.AiClient;
import com.autoshorts.ai.config.AppProperties;
import com.autoshorts.ai.exception.ExternalServiceException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import reactor.core.publisher.Mono;

import java.net.ConnectException;
import java.time.Duration;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeoutException;

@Component
public class WebClientAiClient implements AiClient {

    private final WebClient webClient;
    private final AppProperties appProperties;

    public WebClientAiClient(WebClient.Builder webClientBuilder, AppProperties appProperties) {
        this.appProperties = appProperties;
        this.webClient = webClientBuilder
            .baseUrl(trimTrailingSlash(appProperties.getOpenai().getBaseUrl()))
            .build();
    }

    @Override
    public ChatCompletionResult chatCompletion(String systemPrompt, String userPrompt, Double temperature) {
        validateApiKey();

        Map<String, Object> request = new LinkedHashMap<>();
        request.put("model", resolveModel(null));
        request.put("messages", List.of(
            Map.of("role", "system", "content", safeText(systemPrompt)),
            Map.of("role", "user", "content", safeText(userPrompt))
        ));
        request.put("stream", false);
        if (temperature != null) {
            request.put("temperature", temperature);
        }
        if (expectsJsonResponse(systemPrompt, userPrompt)) {
            request.put("response_format", Map.of("type", "json_object"));
        }

        ChatCompletionResponse response = post(
            resolvePath("/chat/completions"),
            request,
            ChatCompletionResponse.class,
            "chat completions"
        );

        if (response == null || response.choices() == null || response.choices().isEmpty()) {
            throw new ExternalServiceException("AI response is invalid: missing choices");
        }
        Choice firstChoice = response.choices().get(0);
        if (firstChoice == null || firstChoice.message() == null || !StringUtils.hasText(firstChoice.message().content())) {
            throw new ExternalServiceException("AI response is invalid: missing message content");
        }

        String model = StringUtils.hasText(response.model()) ? response.model() : resolveModel(null);
        return new ChatCompletionResult(firstChoice.message().content().trim(), model);
    }

    @Override
    public ImageGenerationResult generateImage(String prompt, String model, String size) {
        validateApiKey();

        Map<String, Object> request = new LinkedHashMap<>();
        request.put("model", resolveModel(model));
        request.put("prompt", safeText(prompt));
        if (StringUtils.hasText(size)) {
            request.put("size", size.trim());
        }

        ImageGenerationResponse response = post(
            resolvePath("/images/generations"),
            request,
            ImageGenerationResponse.class,
            "image generation"
        );

        if (response == null || response.data() == null || response.data().isEmpty()) {
            throw new ExternalServiceException("AI image response is invalid: empty data");
        }

        ImageData first = response.data().get(0);
        if (first == null || !StringUtils.hasText(first.payloadBase64())) {
            throw new ExternalServiceException("AI image response is invalid: missing b64_json payload");
        }

        byte[] imageBytes;
        try {
            imageBytes = Base64.getDecoder().decode(first.payloadBase64());
        } catch (IllegalArgumentException ex) {
            throw new ExternalServiceException("AI image response is invalid: b64_json is not valid base64");
        }

        if (imageBytes.length == 0) {
            throw new ExternalServiceException("AI image response is invalid: decoded image is empty");
        }

        return new ImageGenerationResult(imageBytes, resolveModel(model));
    }

    @Override
    public SpeechSynthesisResult synthesizeSpeech(String input, String model, String voice, String responseFormat) {
        validateApiKey();

        String resolvedModel = resolveSpeechModel(model);
        String resolvedVoice = resolveSpeechVoice(voice, resolvedModel);
        String resolvedFormat = resolveSpeechResponseFormat(responseFormat);

        Map<String, Object> request = new LinkedHashMap<>();
        request.put("model", resolvedModel);
        request.put("input", safeText(input));
        if (StringUtils.hasText(resolvedVoice)) {
            request.put("voice", resolvedVoice);
        }
        if (StringUtils.hasText(resolvedFormat)) {
            request.put("response_format", resolvedFormat);
        }

        byte[] bytes = postBytes(resolvePath("/audio/speech"), request, "speech synthesis", resolveSpeechTimeout());
        if (bytes == null || bytes.length == 0) {
            throw new ExternalServiceException("AI speech response is invalid: empty audio payload");
        }
        return new SpeechSynthesisResult(bytes, resolvedModel, resolvedVoice, resolvedFormat);
    }

    private <T> T post(String path, Object requestBody, Class<T> responseType, String operationName) {
        try {
            return webClient.post()
                .uri(path)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + appProperties.getOpenai().getApiKey().trim())
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .retrieve()
                .onStatus(HttpStatusCode::isError, clientResponse ->
                    clientResponse.bodyToMono(String.class)
                        .defaultIfEmpty("")
                        .flatMap(body -> Mono.error(new ExternalServiceException(
                            "AI %s failed: status=%s body=%s".formatted(
                                operationName,
                                clientResponse.statusCode(),
                                truncate(body, 600)
                            )
                        )))
                )
                .bodyToMono(responseType)
                .block(resolveTimeout());
        } catch (ExternalServiceException ex) {
            throw ex;
        } catch (IllegalStateException ex) {
            if (isTimeout(ex)) {
                throw new ExternalServiceException(
                    "AI request timed out after " + resolveTimeout().toSeconds() + " seconds"
                );
            }
            throw new ExternalServiceException("AI request failed: " + ex.getMessage(), ex);
        } catch (WebClientRequestException ex) {
            if (isConnectionError(ex)) {
                throw new ExternalServiceException(
                    "Cannot reach AI endpoint at %s. Ensure 9Router is running.".formatted(appProperties.getOpenai().getBaseUrl())
                );
            }
            if (isTimeout(ex)) {
                throw new ExternalServiceException(
                    "AI request timed out after " + resolveTimeout().toSeconds() + " seconds"
                );
            }
            throw new ExternalServiceException("AI request failed: " + ex.getMessage(), ex);
        } catch (Exception ex) {
            throw new ExternalServiceException("AI request failed: " + ex.getMessage(), ex);
        }
    }

    private byte[] postBytes(String path, Object requestBody, String operationName, Duration timeout) {
        try {
            return webClient.post()
                .uri(path)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + appProperties.getOpenai().getApiKey().trim())
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_OCTET_STREAM)
                .bodyValue(requestBody)
                .retrieve()
                .onStatus(HttpStatusCode::isError, clientResponse ->
                    clientResponse.bodyToMono(String.class)
                        .defaultIfEmpty("")
                        .flatMap(body -> Mono.error(new ExternalServiceException(
                            "AI %s failed: status=%s body=%s".formatted(
                                operationName,
                                clientResponse.statusCode(),
                                truncate(body, 600)
                            )
                        )))
                )
                .bodyToMono(byte[].class)
                .block(timeout);
        } catch (ExternalServiceException ex) {
            throw ex;
        } catch (IllegalStateException ex) {
            if (isTimeout(ex)) {
                throw new ExternalServiceException(
                    "AI request timed out after " + timeout.toSeconds() + " seconds"
                );
            }
            throw new ExternalServiceException("AI request failed: " + ex.getMessage(), ex);
        } catch (WebClientRequestException ex) {
            if (isConnectionError(ex)) {
                throw new ExternalServiceException(
                    "Cannot reach AI endpoint at %s. Ensure 9Router is running.".formatted(appProperties.getOpenai().getBaseUrl())
                );
            }
            if (isTimeout(ex)) {
                throw new ExternalServiceException(
                    "AI request timed out after " + timeout.toSeconds() + " seconds"
                );
            }
            throw new ExternalServiceException("AI request failed: " + ex.getMessage(), ex);
        } catch (Exception ex) {
            throw new ExternalServiceException("AI request failed: " + ex.getMessage(), ex);
        }
    }

    private boolean expectsJsonResponse(String systemPrompt, String userPrompt) {
        String combined = (safeText(systemPrompt) + "\n" + safeText(userPrompt)).toLowerCase();
        return combined.contains("return strictly valid json")
            || combined.contains("json object")
            || combined.contains("no markdown") && combined.contains("json");
    }

    private void validateApiKey() {
        if (!StringUtils.hasText(appProperties.getOpenai().getApiKey())) {
            throw new ExternalServiceException("Missing AI API key. Set environment variable: apikey");
        }
        if (!StringUtils.hasText(appProperties.getOpenai().getBaseUrl())) {
            throw new ExternalServiceException("Missing AI base URL. Set environment variable: AI_BASE_URL");
        }
    }

    private Duration resolveTimeout() {
        long timeoutMs = Math.max(1000L, appProperties.getOpenai().getRequestTimeoutMs());
        return Duration.ofMillis(timeoutMs);
    }

    private Duration resolveSpeechTimeout() {
        long timeoutMs = Math.max(1000L, appProperties.getOpenai().getSpeechRequestTimeoutMs());
        return Duration.ofMillis(timeoutMs);
    }

    private String resolvePath(String endpointPath) {
        String baseUrl = trimTrailingSlash(appProperties.getOpenai().getBaseUrl());
        if (baseUrl.endsWith("/v1")) {
            return endpointPath;
        }
        return "/v1" + endpointPath;
    }

    private String resolveModel(String requestedModel) {
        if (StringUtils.hasText(requestedModel)) {
            return requestedModel.trim();
        }
        if (StringUtils.hasText(appProperties.getOpenai().getModel())) {
            return appProperties.getOpenai().getModel().trim();
        }
        throw new ExternalServiceException("Missing AI model. Set environment variable: AI_MODEL");
    }

    private String resolveSpeechModel(String requestedModel) {
        if (StringUtils.hasText(requestedModel)) {
            return requestedModel.trim();
        }
        if (StringUtils.hasText(appProperties.getOpenai().getSpeechModel())) {
            return appProperties.getOpenai().getSpeechModel().trim();
        }
        throw new ExternalServiceException("Missing AI TTS model. Set environment variable: AI_TTS_MODEL");
    }

    private String resolveSpeechVoice(String requestedVoice, String resolvedModel) {
        if (StringUtils.hasText(requestedVoice)) {
            return requestedVoice.trim();
        }
        if (StringUtils.hasText(appProperties.getOpenai().getSpeechVoice())) {
            return appProperties.getOpenai().getSpeechVoice().trim();
        }
        if (isGoogleTtsModel(resolvedModel)) {
            return null;
        }
        return "alloy";
    }

    private boolean isGoogleTtsModel(String model) {
        return StringUtils.hasText(model) && model.trim().toLowerCase().startsWith("google-tts/");
    }

    private String resolveSpeechResponseFormat(String requestedFormat) {
        if (StringUtils.hasText(requestedFormat)) {
            return requestedFormat.trim();
        }
        if (StringUtils.hasText(appProperties.getOpenai().getSpeechResponseFormat())) {
            return appProperties.getOpenai().getSpeechResponseFormat().trim();
        }
        return "mp3";
    }

    private boolean isConnectionError(Throwable throwable) {
        Throwable cause = throwable;
        while (cause != null) {
            if (cause instanceof ConnectException) {
                return true;
            }
            cause = cause.getCause();
        }
        return false;
    }

    private boolean isTimeout(Throwable throwable) {
        Throwable cause = throwable;
        while (cause != null) {
            if (cause instanceof TimeoutException) {
                return true;
            }
            String message = cause.getMessage();
            if (message != null && message.toLowerCase().contains("timeout")) {
                return true;
            }
            cause = cause.getCause();
        }
        return false;
    }

    private String safeText(String value) {
        if (!StringUtils.hasText(value)) {
            return "";
        }
        return value.trim();
    }

    private String trimTrailingSlash(String value) {
        if (!StringUtils.hasText(value)) {
            return value;
        }
        String trimmed = value.trim();
        while (trimmed.endsWith("/")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        return trimmed;
    }

    private String truncate(String body, int maxLength) {
        if (body == null) {
            return "";
        }
        if (body.length() <= maxLength) {
            return body;
        }
        return body.substring(0, maxLength);
    }

    private record ChatCompletionResponse(String model, List<Choice> choices) {
    }

    private record Choice(Message message) {
    }

    private record Message(String role, String content) {
    }

    private record ImageGenerationResponse(List<ImageData> data) {
    }

    private record ImageData(String b64_json, String b64Json, String url) {
        private String payloadBase64() {
            return StringUtils.hasText(b64Json) ? b64Json : b64_json;
        }
    }
}
