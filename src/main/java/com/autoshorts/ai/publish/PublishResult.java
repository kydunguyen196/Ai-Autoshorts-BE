package com.autoshorts.ai.publish;

public record PublishResult(
    String provider,
    String externalId,
    String details,
    String requestPayloadJson,
    String responsePayloadJson,
    String targetAccountId,
    boolean pendingAsync
) {

    /**
     * Convenience constructor for synchronous publishers whose post is final once
     * {@link VideoPublisher#publish} returns (e.g. the mock publisher). {@code pendingAsync}
     * defaults to false.
     */
    public PublishResult(
        String provider,
        String externalId,
        String details,
        String requestPayloadJson,
        String responsePayloadJson,
        String targetAccountId
    ) {
        this(provider, externalId, details, requestPayloadJson, responsePayloadJson, targetAccountId, false);
    }
}
