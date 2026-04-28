package com.autoshorts.ai.client.impl;

import com.autoshorts.ai.client.AiClient;
import com.autoshorts.ai.client.ElevenLabsClient;
import com.autoshorts.ai.client.model.SynthesizedAudio;
import com.autoshorts.ai.config.AppProperties;
import com.autoshorts.ai.entity.AudioGenerationMode;
import com.autoshorts.ai.exception.ExternalServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.Instant;
import java.util.Set;

@Component
@ConditionalOnProperty(name = "app.elevenlabs.mock", havingValue = "false", matchIfMissing = true)
public class WebClientElevenLabsClient implements ElevenLabsClient {

    private static final Logger log = LoggerFactory.getLogger(WebClientElevenLabsClient.class);
    private static final Set<String> OPENAI_TTS_VOICES = Set.of(
        "alloy",
        "ash",
        "ballad",
        "coral",
        "echo",
        "fable",
        "nova",
        "onyx",
        "sage",
        "shimmer",
        "verse"
    );

    private final AiClient aiClient;
    private final AppProperties appProperties;

    public WebClientElevenLabsClient(
        AiClient aiClient,
        AppProperties appProperties
    ) {
        this.aiClient = aiClient;
        this.appProperties = appProperties;
    }

    @Override
    public SynthesizedAudio synthesizeSpeech(String text, String requestedVoiceId, int durationSeconds) {
        if (!appProperties.getElevenlabs().isEnabled()) {
            throw new ExternalServiceException("Text to speech is disabled");
        }

        String voiceId = resolveVoiceId(requestedVoiceId);
        String modelId = resolveModelId();
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
            throw new ExternalServiceException("9Router speech synthesis failed: " + ex.getMessage(), ex);
        }
    }

    private String resolveVoiceId(String requestedVoiceId) {
        if (isOpenAiVoice(requestedVoiceId)) {
            return requestedVoiceId.trim();
        }
        if (isOpenAiVoice(appProperties.getOpenai().getSpeechVoice())) {
            return appProperties.getOpenai().getSpeechVoice().trim();
        }
        if (isOpenAiVoice(appProperties.getElevenlabs().getDefaultVoiceId())) {
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

    private boolean isOpenAiVoice(String value) {
        return StringUtils.hasText(value) && OPENAI_TTS_VOICES.contains(value.trim().toLowerCase());
    }
}
