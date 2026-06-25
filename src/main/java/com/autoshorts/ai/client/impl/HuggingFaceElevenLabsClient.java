package com.autoshorts.ai.client.impl;

import com.autoshorts.ai.client.ElevenLabsClient;
import com.autoshorts.ai.client.model.SynthesizedAudio;
import com.autoshorts.ai.config.AppProperties;
import com.autoshorts.ai.entity.AudioGenerationMode;
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
@ConditionalOnExpression("'${app.elevenlabs.mock:false}' == 'false' && '${app.elevenlabs.provider:huggingface}' == 'huggingface'")
public class HuggingFaceElevenLabsClient implements ElevenLabsClient {

    private static final Logger log = LoggerFactory.getLogger(HuggingFaceElevenLabsClient.class);

    private final AppProperties appProperties;
    private final WebClient.Builder webClientBuilder;

    public HuggingFaceElevenLabsClient(AppProperties appProperties, WebClient.Builder webClientBuilder) {
        this.appProperties = appProperties;
        this.webClientBuilder = webClientBuilder;
    }

    @io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker(name = "ai-providers")
    @Override
    public SynthesizedAudio synthesizeSpeech(String text, String requestedVoiceId, int durationSeconds) {
        if (!appProperties.getElevenlabs().isEnabled()) {
            throw new ExternalServiceException("Text to speech is disabled");
        }
        String model = resolveModel();
        String provider = resolveProvider();
        String outputFormat = resolveOutputFormat();
        String extension = HuggingFaceSupport.normalizeAudioExtension(outputFormat);
        MediaType contentType = HuggingFaceSupport.mediaTypeForAudioFormat(outputFormat);
        WebClient client = HuggingFaceSupport.webClient(webClientBuilder, appProperties);
        Instant startedAt = Instant.now();

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("inputs", text == null ? "" : text);

        byte[] bytes = client
            .post()
            .uri(HuggingFaceSupport.providerModelPath(provider, model))
            .header(HttpHeaders.ACCEPT, contentType.toString())
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(body)
            .retrieve()
            .onStatus(HuggingFaceSupport::isError, HuggingFaceSupport::errorFromResponse)
            .bodyToMono(byte[].class)
            .block(Duration.ofMillis(Math.max(1000L, appProperties.getHuggingface().getRequestTimeoutMs())));

        if (bytes == null || bytes.length == 0) {
            throw new ExternalServiceException("Hugging Face TTS response is invalid: empty audio payload");
        }
        long durationMs = Duration.between(startedAt, Instant.now()).toMillis();
        log.info(
            "event=huggingface_tts_success provider={} model={} outputFormat={} bytes={} durationMs={}",
            provider,
            model,
            outputFormat,
            bytes.length,
            durationMs
        );

        return new SynthesizedAudio(
            bytes,
            extension,
            contentType.toString(),
            AudioGenerationMode.REAL,
            "HUGGINGFACE_TTS",
            "real_success",
            requestedVoiceId,
            model,
            outputFormat,
            durationMs,
            null,
            null
        );
    }

    private String resolveModel() {
        String model = HuggingFaceSupport.trimToNull(appProperties.getHuggingface().getTtsModel());
        if (!StringUtils.hasText(model)) {
            model = HuggingFaceSupport.trimToNull(appProperties.getElevenlabs().getDefaultModelId());
        }
        if (!StringUtils.hasText(model)) {
            throw new ExternalServiceException("Missing HF_TTS_MODEL");
        }
        return model;
    }

    private String resolveProvider() {
        String provider = HuggingFaceSupport.trimToNull(appProperties.getHuggingface().getTtsProvider());
        return StringUtils.hasText(provider) ? provider : "hf-inference";
    }

    private String resolveOutputFormat() {
        String outputFormat = HuggingFaceSupport.trimToNull(appProperties.getElevenlabs().getOutputFormat());
        return StringUtils.hasText(outputFormat) ? outputFormat : "mp3";
    }
}
