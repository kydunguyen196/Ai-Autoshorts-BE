package com.autoshorts.ai.client.model;

import com.autoshorts.ai.entity.ContentGenerationMode;

public record OpenAiGenerationResult(
    String content,
    ContentGenerationMode mode,
    String provider,
    String model
) {
}

