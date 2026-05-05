package com.autoshorts.ai.client.impl;

import com.autoshorts.ai.client.AiClient;
import com.autoshorts.ai.client.ElevenLabsClient;
import com.autoshorts.ai.client.model.SynthesizedAudio;
import com.autoshorts.ai.config.AppProperties;
import com.autoshorts.ai.entity.AudioGenerationMode;
import com.autoshorts.ai.exception.ExternalServiceException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
@ConditionalOnProperty(name = "app.elevenlabs.mock", havingValue = "false", matchIfMissing = true)
public class WebClientElevenLabsClient implements ElevenLabsClient {

    private static final Logger log = LoggerFactory.getLogger(WebClientElevenLabsClient.class);
    private final AiClient aiClient;
    private final AppProperties appProperties;
    private final WebClient.Builder webClientBuilder;

    public WebClientElevenLabsClient(
        AiClient aiClient,
        AppProperties appProperties,
        WebClient.Builder webClientBuilder
    ) {
        this.aiClient = aiClient;
        this.appProperties = appProperties;
        this.webClientBuilder = webClientBuilder;
    }

    @Override
    public SynthesizedAudio synthesizeSpeech(String text, String requestedVoiceId, int durationSeconds) {
        if (!appProperties.getElevenlabs().isEnabled()) {
            throw new ExternalServiceException("Text to speech is disabled");
        }

        String modelId = resolveModelId();
        String voiceId = resolveVoiceId(requestedVoiceId, modelId);
        String outputFormat = resolveOutputFormat();
        String extension = resolveFileExtension(outputFormat);
        String contentType = resolveContentType(extension);
        Instant startedAt = Instant.now();

        try {
            AiClient.SpeechSynthesisResult result = aiClient.synthesizeSpeech(
                text,
                modelId,
                voiceId,
                outputFormat
            );
            long durationMs = Duration.between(startedAt, Instant.now()).toMillis();
            byte[] bytes = result.data();
            if (bytes == null || bytes.length == 0) {
                throw new ExternalServiceException("9Router returned empty audio response");
            }

            log.info(
                "event=ai_tts_request_success provider=9router_openai_compatible voice={} model={} outputFormat={} bytes={} durationMs={}",
                result.voice(),
                result.model(),
                result.responseFormat(),
                bytes.length,
                durationMs
            );

            return new SynthesizedAudio(
                bytes,
                extension,
                contentType,
                AudioGenerationMode.REAL,
                "9ROUTER_TTS",
                "real_success",
                result.voice(),
                result.model(),
                result.responseFormat(),
                durationMs,
                null,
                null
            );
        } catch (Exception ex) {
            long durationMs = Duration.between(startedAt, Instant.now()).toMillis();
            log.warn(
                "event=ai_tts_request_failed provider=9router_openai_compatible voice={} model={} outputFormat={} durationMs={} message={}",
                voiceId,
                modelId,
                outputFormat,
                durationMs,
                ex.getMessage()
            );
            if (isGoogleTtsModel(modelId)) {
                return synthesizeGoogleTtsWithShortenedInput(text, modelId, outputFormat, ex);
            }
            return synthesizeViaElevenLabsDirect(text, requestedVoiceId, outputFormat, ex);
        }
    }

    private String resolveVoiceId(String requestedVoiceId, String modelId) {
        if (isGoogleTtsModel(modelId)) {
            return null;
        }
        if (StringUtils.hasText(requestedVoiceId)) {
            return requestedVoiceId.trim();
        }
        if (StringUtils.hasText(appProperties.getOpenai().getSpeechVoice())) {
            return appProperties.getOpenai().getSpeechVoice().trim();
        }
        if (StringUtils.hasText(appProperties.getElevenlabs().getDefaultVoiceId())) {
            return appProperties.getElevenlabs().getDefaultVoiceId().trim();
        }
        return "alloy";
    }

    private String resolveModelId() {
        if (StringUtils.hasText(appProperties.getOpenai().getSpeechModel())) {
            return appProperties.getOpenai().getSpeechModel().trim();
        }
        if (StringUtils.hasText(appProperties.getElevenlabs().getDefaultModelId())) {
            return appProperties.getElevenlabs().getDefaultModelId().trim();
        }
        return "openrouter/openai/gpt-4o-mini-tts";
    }

    private String resolveOutputFormat() {
        if (StringUtils.hasText(appProperties.getOpenai().getSpeechResponseFormat())) {
            return appProperties.getOpenai().getSpeechResponseFormat().trim();
        }
        if (StringUtils.hasText(appProperties.getElevenlabs().getOutputFormat())) {
            return appProperties.getElevenlabs().getOutputFormat().trim();
        }
        return "mp3";
    }

    private String resolveFileExtension(String outputFormat) {
        String lower = outputFormat == null ? "" : outputFormat.toLowerCase();
        if (lower.startsWith("pcm") || lower.startsWith("wav")) {
            return "wav";
        }
        if (lower.startsWith("opus")) {
            return "opus";
        }
        if (lower.startsWith("aac")) {
            return "aac";
        }
        if (lower.startsWith("flac")) {
            return "flac";
        }
        return "mp3";
    }

    private String resolveContentType(String extension) {
        return switch (extension) {
            case "wav" -> "audio/wav";
            case "opus" -> "audio/opus";
            case "aac" -> "audio/aac";
            case "flac" -> "audio/flac";
            default -> "audio/mpeg";
        };
    }

    private boolean isGoogleTtsModel(String modelId) {
        return StringUtils.hasText(modelId) && modelId.trim().toLowerCase().startsWith("google-tts/");
    }

    private SynthesizedAudio synthesizeViaElevenLabsDirect(
        String text,
        String requestedVoiceId,
        String outputFormat,
        Exception upstreamException
    ) {
        if (!StringUtils.hasText(appProperties.getElevenlabs().getApiKey())) {
            throw new ExternalServiceException("9Router speech synthesis failed: " + upstreamException.getMessage(), upstreamException);
        }

        String voiceId = StringUtils.hasText(requestedVoiceId)
            ? requestedVoiceId.trim()
            : trimToNull(appProperties.getElevenlabs().getDefaultVoiceId());
        if (!StringUtils.hasText(voiceId)) {
            throw new ExternalServiceException("9Router speech synthesis failed and ELEVENLABS_DEFAULT_VOICE_ID is missing", upstreamException);
        }

        String modelId = trimToNull(appProperties.getElevenlabs().getDefaultModelId());
        if (!StringUtils.hasText(modelId)) {
            modelId = "eleven_v3";
        }
        String resolvedOutput = normalizeElevenLabsOutputFormat(outputFormat);
        Instant startedAt = Instant.now();
        try {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("text", text == null ? "" : text);
            body.put("model_id", modelId);

            byte[] bytes = webClientBuilder
                .baseUrl(trimTrailingSlash(appProperties.getElevenlabs().getBaseUrl()))
                .build()
                .post()
                .uri("/v1/text-to-speech/{voiceId}?output_format={format}", voiceId, resolvedOutput)
                .header("xi-api-key", appProperties.getElevenlabs().getApiKey().trim())
                .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_OCTET_STREAM_VALUE)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .retrieve()
                .onStatus(HttpStatusCode::isError, clientResponse ->
                    clientResponse.bodyToMono(String.class)
                        .defaultIfEmpty("")
                        .flatMap(responseBody -> Mono.error(new ExternalServiceException(
                            "ElevenLabs direct failed: status=%s body=%s".formatted(clientResponse.statusCode(), truncate(responseBody, 600))
                        )))
                )
                .bodyToMono(byte[].class)
                .block(Duration.ofMillis(Math.max(1000L, appProperties.getElevenlabs().getRequestTimeoutMs())));

            if (bytes == null || bytes.length == 0) {
                throw new ExternalServiceException("ElevenLabs direct failed: empty audio payload");
            }
            long durationMs = Duration.between(startedAt, Instant.now()).toMillis();
            log.info(
                "event=ai_tts_request_success provider=elevenlabs_direct voice={} model={} outputFormat={} bytes={} durationMs={}",
                voiceId,
                modelId,
                resolvedOutput,
                bytes.length,
                durationMs
            );

            String extension = resolveFileExtension(resolvedOutput);
            String contentType = resolveContentType(extension);
            return new SynthesizedAudio(
                bytes,
                extension,
                contentType,
                AudioGenerationMode.REAL,
                "ELEVENLABS_DIRECT",
                "real_success",
                voiceId,
                modelId,
                resolvedOutput,
                durationMs,
                null,
                null
            );
        } catch (Exception directEx) {
            throw new ExternalServiceException(
                "9Router speech synthesis failed: " + upstreamException.getMessage() + " | ElevenLabs direct fallback failed: " + directEx.getMessage(),
                directEx
            );
        }
    }

    private String normalizeElevenLabsOutputFormat(String outputFormat) {
        String value = trimToNull(outputFormat);
        if (!StringUtils.hasText(value)) {
            value = trimToNull(appProperties.getElevenlabs().getOutputFormat());
        }
        if (!StringUtils.hasText(value)) {
            return "mp3_44100_128";
        }
        if ("mp3".equalsIgnoreCase(value)) {
            return "mp3_44100_128";
        }
        return value;
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private SynthesizedAudio synthesizeGoogleTtsWithShortenedInput(
        String text,
        String modelId,
        String outputFormat,
        Exception upstreamException
    ) {
        String shortened = shortenForGoogleTts(text);
        if (!StringUtils.hasText(shortened)) {
            throw new ExternalServiceException("9Router speech synthesis failed: " + upstreamException.getMessage(), upstreamException);
        }
        Instant startedAt = Instant.now();
        try {
            AiClient.SpeechSynthesisResult result = aiClient.synthesizeSpeech(shortened, modelId, null, outputFormat);
            long durationMs = Duration.between(startedAt, Instant.now()).toMillis();
            byte[] bytes = result.data();
            if (bytes == null || bytes.length == 0) {
                throw new ExternalServiceException("9Router returned empty audio response");
            }
            log.info(
                "event=ai_tts_request_success provider=9router_openai_compatible voice={} model={} outputFormat={} bytes={} durationMs={} retryMode=shortened_input",
                result.voice(),
                result.model(),
                result.responseFormat(),
                bytes.length,
                durationMs
            );
            String extension = resolveFileExtension(result.responseFormat());
            String contentType = resolveContentType(extension);
            return new SynthesizedAudio(
                bytes,
                extension,
                contentType,
                AudioGenerationMode.REAL,
                "9ROUTER_TTS",
                "real_success_shortened_input",
                result.voice(),
                result.model(),
                result.responseFormat(),
                durationMs,
                null,
                null
            );
        } catch (Exception retryEx) {
            throw new ExternalServiceException(
                "9Router speech synthesis failed: " + upstreamException.getMessage() + " | shortened-input retry failed: " + retryEx.getMessage(),
                retryEx
            );
        }
    }

    private String shortenForGoogleTts(String text) {
        String value = trimToNull(text);
        if (!StringUtils.hasText(value)) {
            return null;
        }
        String normalized = value.replaceAll("\\s+", " ").trim();
        if (normalized.length() <= 220) {
            return normalized;
        }
        int punctuationIndex = Math.max(
            normalized.lastIndexOf('.', 220),
            Math.max(normalized.lastIndexOf('!', 220), normalized.lastIndexOf('?', 220))
        );
        if (punctuationIndex >= 120) {
            return normalized.substring(0, punctuationIndex + 1);
        }
        return normalized.substring(0, 220);
    }

    private String trimTrailingSlash(String value) {
        if (!StringUtils.hasText(value)) {
            return value;
        }
        String result = value.trim();
        while (result.endsWith("/")) {
            result = result.substring(0, result.length() - 1);
        }
        return result;
    }

    private String truncate(String value, int max) {
        if (value == null) {
            return "";
        }
        if (value.length() <= max) {
            return value;
        }
        return value.substring(0, max);
    }
}
