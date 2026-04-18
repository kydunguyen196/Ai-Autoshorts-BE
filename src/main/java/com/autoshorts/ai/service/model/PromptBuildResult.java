package com.autoshorts.ai.service.model;

import java.util.UUID;

public record PromptBuildResult(
    UUID templateId,
    String resolvedStyle,
    String styleVariantKey,
    String selectedHookStrategy,
    String selectedCtaStrategy,
    String selectedStructureStrategy,
    String systemPrompt,
    String userPrompt
) {
}
