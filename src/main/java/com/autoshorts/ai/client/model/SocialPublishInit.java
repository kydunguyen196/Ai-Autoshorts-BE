package com.autoshorts.ai.client.model;

/**
 * Result of initializing an async social post (YouTube upload / Instagram media container).
 * {@code publishId} is the handle used to poll final processing status.
 */
public record SocialPublishInit(
    String publishId,
    String rawResponseJson
) {
}
