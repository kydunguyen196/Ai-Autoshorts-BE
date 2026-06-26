package com.autoshorts.ai.client;

import com.autoshorts.ai.client.model.SocialPublishInit;
import com.autoshorts.ai.client.model.SocialPublishStatus;

/**
 * Thin abstraction over the YouTube Data API v3 for uploading Shorts. Only invoked when
 * {@code app.youtube.direct-publish-enabled=true}; otherwise the publisher runs in scaffold mode.
 */
public interface YoutubeApiClient {

    /**
     * Upload a video as a Short (resumable upload of the bytes behind {@code videoUrl}).
     *
     * @return the YouTube videoId plus the raw insert response
     */
    SocialPublishInit uploadShort(String accessToken, String videoUrl, String title, String description);

    /** Poll {@code videos.list?part=status,processingDetails} for the upload/processing status. */
    SocialPublishStatus fetchUploadStatus(String accessToken, String videoId);
}
