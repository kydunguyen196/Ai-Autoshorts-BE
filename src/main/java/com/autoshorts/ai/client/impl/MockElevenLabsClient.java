package com.autoshorts.ai.client.impl;

import com.autoshorts.ai.audio.DeterministicPreviewAudioGenerator;
import com.autoshorts.ai.client.ElevenLabsClient;
import com.autoshorts.ai.client.model.SynthesizedAudio;
import com.autoshorts.ai.config.AppProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@ConditionalOnProperty(name = "app.elevenlabs.mock", havingValue = "true")
public class MockElevenLabsClient implements ElevenLabsClient {

    private final DeterministicPreviewAudioGenerator previewAudioGenerator;
    private final AppProperties appProperties;

    public MockElevenLabsClient(
        DeterministicPreviewAudioGenerator previewAudioGenerator,
        AppProperties appProperties
    ) {
        this.previewAudioGenerator = previewAudioGenerator;
        this.appProperties = appProperties;
    }

    @Override
    public SynthesizedAudio synthesizeSpeech(String text, String requestedVoiceId, int durationSeconds) {
        SynthesizedAudio base = previewAudioGenerator.generateMockSpeech(text, durationSeconds);
        String resolvedVoiceId = StringUtils.hasText(requestedVoiceId)
            ? requestedVoiceId.trim()
            : trimToNull(appProperties.getElevenlabs().getDefaultVoiceId());
        String modelId = trimToNull(appProperties.getElevenlabs().getDefaultModelId());
        String outputFormat = trimToNull(appProperties.getElevenlabs().getOutputFormat());

        return new SynthesizedAudio(
            base.getData(),
            base.getFileExtension(),
            base.getContentType(),
            base.getGenerationMode(),
            base.getProvider(),
            base.getModeDetail(),
            resolvedVoiceId,
            modelId,
            outputFormat,
            0L,
            null,
            null
        );
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }
}
