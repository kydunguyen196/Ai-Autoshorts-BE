package com.autoshorts.ai.service.model;

import java.util.UUID;

public record CharacterGenerationContext(
    UUID characterProfileId,
    String characterName,
    String characterArchetype,
    String personality,
    String toneOfVoice,
    String speakingStyle,
    String catchphrases,
    String visualStyle,
    String language,
    String profileTargetAudience,
    String allowedTopics,
    String forbiddenTopics,
    String defaultVoiceProvider,
    String defaultVoiceId,
    UUID characterCampaignId,
    String productName,
    String productType,
    String productDescription,
    String productUrl,
    String targetPlatform,
    String campaignObjective,
    String campaignCallToAction,
    String campaignTargetAudience,
    String offerSummary,
    String storyAngle,
    String productPlacementMode,
    String adDisclosureMode,
    Integer sceneCountTarget,
    String characterConsistencyMode
) {

    public boolean hasAnyContext() {
        return characterProfileId != null
            || characterCampaignId != null
            || isPresent(storyAngle)
            || isPresent(productPlacementMode)
            || isPresent(adDisclosureMode)
            || sceneCountTarget != null
            || isPresent(characterConsistencyMode);
    }

    private boolean isPresent(String value) {
        return value != null && !value.isBlank();
    }
}
