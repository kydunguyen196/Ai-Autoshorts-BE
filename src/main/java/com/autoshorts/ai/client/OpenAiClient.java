package com.autoshorts.ai.client;

import com.autoshorts.ai.client.model.OpenAiGenerationResult;

public interface OpenAiClient {

    OpenAiGenerationResult generateShortVideoScript(String topic, String style, int durationSeconds);

    OpenAiGenerationResult generateFromPrompts(String systemPrompt, String userPrompt);
}
