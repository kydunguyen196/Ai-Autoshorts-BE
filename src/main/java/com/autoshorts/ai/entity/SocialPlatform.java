package com.autoshorts.ai.entity;

import java.util.Optional;

/**
 * Social networks supported by the multi-platform publish pipeline (Phase 3).
 * TikTok keeps its dedicated {@code tiktok_account_connections} table/flow; these are the
 * networks backed by the generic {@code social_account_connections} table.
 */
public enum SocialPlatform {
    YOUTUBE("youtube"),
    INSTAGRAM("instagram");

    private final String key;

    SocialPlatform(String key) {
        this.key = key;
    }

    /** Lowercase provider/platform key used in URLs, config and {@code VideoPublisher.providerKey()}. */
    public String key() {
        return key;
    }

    public static Optional<SocialPlatform> fromKey(String value) {
        if (value == null) {
            return Optional.empty();
        }
        String normalized = value.trim().toLowerCase();
        for (SocialPlatform platform : values()) {
            if (platform.key.equals(normalized)) {
                return Optional.of(platform);
            }
        }
        return Optional.empty();
    }
}
