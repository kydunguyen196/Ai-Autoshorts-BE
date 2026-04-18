package com.autoshorts.ai.service.model;

import com.autoshorts.ai.entity.ContentGenerationMode;

import java.util.List;
import java.util.UUID;

public record GeneratedContent(
    UUID promptTemplateId,
    String resolvedStyle,
    String styleVariantKey,
    String hookStrategy,
    String ctaStrategy,
    String structureStrategy,
    Integer hookStrengthScore,
    Integer engagementScore,
    String engagementTagsJson,
    ContentGenerationMode generationMode,
    String hookText,
    String scriptText,
    String ctaText,
    String captionText,
    List<String> hashtags,
    String sceneBreakdownJson
) {
}
