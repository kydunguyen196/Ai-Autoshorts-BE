package com.autoshorts.ai.client;

import com.autoshorts.ai.client.model.SocialPublishInit;
import com.autoshorts.ai.client.model.SocialPublishStatus;

/**
 * Thin abstraction over the Instagram Graph API for publishing Reels. Only invoked when
 * {@code app.instagram.direct-publish-enabled=true}; otherwise the publisher runs in scaffold mode.
 */
public interface InstagramApiClient {

    /**
     * Create a REELS media container for the (publicly hosted) {@code videoUrl}.
     * The container must finish processing before it can be published.
     *
     * @return the container id plus the raw create response
     */
    SocialPublishInit createReelContainer(String accessToken, String igUserId, String videoUrl, String caption);

    /** Poll {@code GET /{containerId}?fields=status_code} until FINISHED/ERROR. */
    SocialPublishStatus fetchContainerStatus(String accessToken, String containerId);

    /** Publish a FINISHED container via {@code POST /{igUserId}/media_publish}. Returns the media id. */
    SocialPublishInit publishContainer(String accessToken, String igUserId, String containerId);
}
