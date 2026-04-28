package com.autoshorts.ai.client;

import com.autoshorts.ai.client.model.GeneratedVisualImage;

public interface VisualGenerationClient {

    GeneratedVisualImage generateSceneImage(String prompt, int sceneIndex);
}
