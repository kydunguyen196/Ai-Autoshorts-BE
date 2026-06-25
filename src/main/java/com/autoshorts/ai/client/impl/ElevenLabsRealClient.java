package com.autoshorts.ai.client.impl;

import com.autoshorts.ai.client.ElevenLabsClient;
import com.autoshorts.ai.client.model.SynthesizedAudio;
import com.autoshorts.ai.config.AppProperties;
import com.autoshorts.ai.entity.AudioGenerationMode;
import com.autoshorts.ai.exception.ExternalServiceException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Real ElevenLabs text-to-speech provider. Activated with ELEVENLABS_PROVIDER=elevenlabs
 * (and ELEVENLABS_MOCK=false). Requires ELEVENLABS_API_KEY and a default/requested voice id.
 */
@Component
@ConditionalOnExpression("'${app.elevenlabs.mock:false}' == 'false' && '${app.elevenlabs.provider:huggingface}' == 'elevenlabs'")
public class ElevenLabsRealClient implements ElevenLabsClient {

    private static final Logger log = LoggerFactory.getLogger(ElevenLabsRealClient.class);
    private static final String DEFAULT_BASE_URL = "https://api.elevenlabs.io";

    private final AppProperties appProperties;
    private final WebClient.Builder webClientBuilder;
    private final String modelId;
    private final String defaultVoiceId;
    private final String outputFormat;

    public ElevenLabsRealClient(
        AppProperties appProperties,
        WebClient.Builder webClientBuilder,
        @Value("${ELEVENLABS_REAL_MODEL_ID:eleven_multilingual_v2}") String modelId,
        @Value("${ELEVENLABS_DEFAULT_VOICE_ID:}") String defaultVoiceId,
        @Value("${ELEVENLABS_REAL_OUTPUT_FORMAT:mp3_44100_128}") String outputFormat
    ) {
        this.appProperties = appProperties;
        this.webClientBuilder = webClientBuilder;
        this.modelId = modelId;
        this.defaultVoiceId = defaultVoiceId;
        this.outputFormat = outputFormat;
    }

    @CircuitBreaker(name = "ai-providers")
    @Override
    public SynthesizedAudio synthesizeSpeech(String text, String requestedVoiceId, int durationSeconds) {
        if (!appProperties.getElevenlabs().isEnabled()) {
            throw new ExternalServiceException("Text to speech is disabled");
        }
        String apiKey = appProperties.getElevenlabs().getApiKey();
        if (!StringUtils.hasText(apiKey)) {
            throw new ExternalServiceException("Missing ELEVENLABS_API_KEY");
        }
        String voiceId = StringUtils.hasText(requestedVoiceId) ? requestedVoiceId : defaultVoiceId;
        if (!StringUtils.hasText(voiceId)) {
            throw new ExternalServiceException("Missing ElevenLabs voice id (set ELEVENLABS_DEFAULT_VOICE_ID)");
        }

        String baseUrl = StringUtils.hasText(appProperties.getElevenlabs().getBaseUrl())
            ? appProperties.getElevenlabs().getBaseUrl()
            : DEFAULT_BASE_URL;
        WebClient client = webClientBuilder.baseUrl(baseUrl).build();

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("text", text == null ? "" : text);
        body.put("model_id", modelId);

        Instant startedAt = Instant.now();
        byte[] bytes = client
            .post()
            .uri(uriBuilder -> uriBuilder
                .path("/v1/text-to-speech/{voiceId}")
                .queryParam("output_format", outputFormat)
                .build(voiceId))
            .header("xi-api-key", apiKey)
            .accept(MediaType.valueOf("audio/mpeg"))
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(body)
            .retrieve()
            .onStatus(HuggingFaceSupport::isError, HuggingFaceSupport::errorFromResponse)
            .bodyToMono(byte[].class)
            .block(Duration.ofMillis(Math.max(1000L, appProperties.getElevenlabs().getRequestTimeoutMs())));

        if (bytes == null || bytes.length == 0) {
            throw new ExternalServiceException("ElevenLabs TTS response is invalid: empty audio payload");
        }
        long durationMs = Duration.between(startedAt, Instant.now()).toMillis();
        log.info(
            "event=elevenlabs_tts_success voiceId={} model={} bytes={} durationMs={}",
            voiceId, modelId, bytes.length, durationMs
        );

        return new SynthesizedAudio(
            bytes,
            "mp3",
            "audio/mpeg",
            AudioGenerationMode.REAL,
            "ELEVENLABS_TTS",
            "real_success",
            voiceId,
            modelId,
            outputFormat,
            durationMs,
            null,
            null
        );
    }
}
