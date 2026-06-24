package com.autoshorts.ai.client.model;

import java.util.List;

/**
 * Subset of the TikTok creator info response needed to validate a post before publishing.
 */
public record TikTokCreatorInfo(
    String creatorUsername,
    String creatorNickname,
    List<String> privacyLevelOptions,
    int maxVideoPostDurationSec,
    boolean commentDisabled,
    boolean duetDisabled,
    boolean stitchDisabled
) {
}
