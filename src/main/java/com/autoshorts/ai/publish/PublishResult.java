package com.autoshorts.ai.publish;

public record PublishResult(
    String provider,
    String externalId,
    String details,
    String requestPayloadJson,
    String responsePayloadJson,
    String targetAccountId
) {
}
