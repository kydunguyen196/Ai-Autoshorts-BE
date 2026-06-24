package com.autoshorts.ai.client.model;

/**
 * Result of initializing a TikTok direct post via PULL_FROM_URL.
 * {@code publishId} is the handle used to poll final processing status.
 */
public record TikTokPublishInit(
    String publishId,
    String rawResponseJson
) {
}
