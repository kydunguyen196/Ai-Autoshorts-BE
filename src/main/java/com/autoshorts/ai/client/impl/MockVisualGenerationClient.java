package com.autoshorts.ai.client.impl;

import com.autoshorts.ai.client.VisualGenerationClient;
import com.autoshorts.ai.client.model.GeneratedVisualImage;
import com.autoshorts.ai.visual.DeterministicPreviewVisualGenerator;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.visual.mock", havingValue = "true", matchIfMissing = true)
public class MockVisualGenerationClient implements VisualGenerationClient {

    private final DeterministicPreviewVisualGenerator previewVisualGenerator;

    public MockVisualGenerationClient(DeterministicPreviewVisualGenerator previewVisualGenerator) {
        this.previewVisualGenerator = previewVisualGenerator;
    }

    @Override
    public GeneratedVisualImage generateSceneImage(String prompt, int sceneIndex) {
        return previewVisualGenerator.generateMockImage(prompt, sceneIndex);
    }
}
