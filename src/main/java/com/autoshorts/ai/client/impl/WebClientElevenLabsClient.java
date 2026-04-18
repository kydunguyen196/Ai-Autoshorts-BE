package com.autoshorts.ai.client.impl;

import com.autoshorts.ai.client.ElevenLabsClient;
import com.autoshorts.ai.client.model.SynthesizedAudio;
import com.autoshorts.ai.config.AppProperties;
import com.autoshorts.ai.entity.AudioGenerationMode;
import com.autoshorts.ai.exception.ExternalServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;

@Component
@ConditionalOnProperty(name = "app.elevenlabs.mock", havingValue = "false", matchIfMissing = true)
public class WebClientElevenLabsClient implements ElevenLabsClient {

    private static final Logger log = LoggerFactory.getLogger(WebClientElevenLabsClient.class);

    private final WebClient webClient;
    private final AppProperties appProperties;

    public WebClientElevenLabsClient(
        WebClient.Builder webClientBuilder,
        AppProperties appProperties
    ) {
        this.appProperties = appProperties;
        this.webClient = webClientBuilder
            .baseUrl(appProperties.getElevenlabs().getBaseUrl())
            .build();
    }

    @Override
    public SynthesizedAudio synthesizeSpeech(String text, String requestedVoiceId, int durationSeconds) {
        if (!StringUtils.hasText(appProperties.getElevenlabs().getApiKey())) {
            throw new ExternalServiceException("ElevenLabs API key is missing");
        }
        if (!StringUtils.hasText(requestedVoiceId)) {
            throw new ExternalServiceException("ElevenLabs voiceId is required");
        }

        String voiceId = requestedVoiceId.trim();
        String modelId = resolveModelId();
        String outputFormat = resolveOutputFormat();
        String extension = resolveFileExtension(outputFormat);
        String contentType = resolveContentType(extension);

        Map<String, Object> request = Map.of(
            "text", text,
            "model_id", modelId,
            "output_format", outputFormat,
            "voice_settings", Map.of(
                "stability", 0.45,
                "similarity_boost", 0.8
            )
        );

        Instant startedAt = Instant.now();
        try {
            byte[] bytes = webClient.post()
                .uri("/v1/text-to-speech/{voiceId}", voiceId)
                .header("xi-api-key", appProperties.getElevenlabs().getApiKey())
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_OCTET_STREAM)
                .bodyValue(request)
                .retrieve()
                .onStatus(HttpStatusCode::isError, clientResponse ->
                    clientResponse.bodyToMono(String.class)
                        .defaultIfEmpty("")
                        .flatMap(body -> Mono.error(new ExternalServiceException(
                            "ElevenLabs request failed: status=%s body=%s".formatted(
                                clientResponse.statusCode(),
                                truncateBody(body, 400)
                            )
                        )))
                )
                .bodyToMono(byte[].class)
                .block(Duration.ofMillis(Math.max(500, appProperties.getElevenlabs().getRequestTimeoutMs())));

            long durationMs = Duration.between(startedAt, Instant.now()).toMillis();
            if (bytes == null || bytes.length == 0) {
                throw new ExternalServiceException("ElevenLabs returned empty audio response");
            }

            log.info(
                "event=elevenlabs_request_success voiceId={} modelId={} outputFormat={} bytes={} durationMs={}",
                voiceId,
                modelId,
                outputFormat,
                bytes.length,
                durationMs
            );

            return new SynthesizedAudio(
                bytes,
                extension,
                contentType,
                AudioGenerationMode.REAL,
                "ELEVENLABS",
                "real_success",
                voiceId,
                modelId,
                outputFormat,
                durationMs,
                null,
                null
            );
        } catch (Exception ex) {
            long durationMs = Duration.between(startedAt, Instant.now()).toMillis();
            log.warn(
                "event=elevenlabs_request_failed voiceId={} modelId={} outputFormat={} durationMs={} message={}",
                voiceId,
                modelId,
                outputFormat,
                durationMs,
                ex.getMessage()
            );
            throw new ExternalServiceException("ElevenLabs synthesis failed: " + ex.getMessage());
        }
    }

    private String resolveModelId() {
        if (StringUtils.hasText(appProperties.getElevenlabs().getDefaultModelId())) {
            return appProperties.getElevenlabs().getDefaultModelId().trim();
        }
        return "eleven_v3";
    }

    private String resolveOutputFormat() {
        if (StringUtils.hasText(appProperties.getElevenlabs().getOutputFormat())) {
            return appProperties.getElevenlabs().getOutputFormat().trim();
        }
        return "mp3_44100_128";
    }

    private String resolveFileExtension(String outputFormat) {
        String lower = outputFormat == null ? "" : outputFormat.toLowerCase();
        if (lower.startsWith("pcm") || lower.startsWith("ulaw") || lower.startsWith("alaw")) {
            return "wav";
        }
        if (lower.startsWith("opus")) {
            return "opus";
        }
        return "mp3";
    }

    private String resolveContentType(String extension) {
        return switch (extension) {
            case "wav" -> "audio/wav";
            case "opus" -> "audio/opus";
            default -> "audio/mpeg";
        };
    }

    private String truncateBody(String body, int maxLength) {
        if (body == null) {
            return "";
        }
        if (body.length() <= maxLength) {
            return body;
        }
        return body.substring(0, maxLength);
    }
}
