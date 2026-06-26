package com.autoshorts.ai.client.model;

import java.util.Set;

/**
 * Snapshot of an async social post's processing status, normalized across providers.
 *
 * @param status       raw provider status string
 * @param publicPostId the published id/url once available (may be null)
 * @param failReason   error reason when failed (may be null)
 */
public record SocialPublishStatus(
    String status,
    String publicPostId,
    String failReason,
    String rawResponseJson
) {

    private static final Set<String> SUCCESS = Set.of(
        // YouTube processingStatus / uploadStatus
        "PROCESSED", "SUCCEEDED", "UPLOADED",
        // Instagram container status_code
        "FINISHED", "PUBLISHED"
    );

    private static final Set<String> FAILURE = Set.of(
        "FAILED", "ERROR", "REJECTED", "EXPIRED"
    );

    public boolean isTerminalSuccess() {
        return status != null && SUCCESS.contains(status.toUpperCase());
    }

    public boolean isTerminalFailure() {
        return status != null && FAILURE.contains(status.toUpperCase());
    }
}
