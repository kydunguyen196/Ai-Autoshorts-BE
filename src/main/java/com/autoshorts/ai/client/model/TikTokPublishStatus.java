package com.autoshorts.ai.client.model;

/**
 * Snapshot of a TikTok post's processing status.
 *
 * @param status   raw TikTok status (e.g. PROCESSING_UPLOAD, PUBLISH_COMPLETE, FAILED)
 * @param publicPostId the published video/post id once available (may be null)
 * @param failReason   error reason when the status is a failure (may be null)
 */
public record TikTokPublishStatus(
    String status,
    String publicPostId,
    String failReason,
    String rawResponseJson
) {

    public boolean isTerminalSuccess() {
        return "PUBLISH_COMPLETE".equalsIgnoreCase(status) || "SEND_TO_USER_INBOX".equalsIgnoreCase(status);
    }

    public boolean isTerminalFailure() {
        return "FAILED".equalsIgnoreCase(status);
    }
}
