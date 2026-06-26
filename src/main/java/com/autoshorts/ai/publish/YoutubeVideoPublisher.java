package com.autoshorts.ai.publish;

import com.autoshorts.ai.client.YoutubeApiClient;
import com.autoshorts.ai.client.model.SocialPublishInit;
import com.autoshorts.ai.config.AppProperties;
import com.autoshorts.ai.entity.SocialPlatform;
import com.autoshorts.ai.exception.ExternalServiceException;
import com.autoshorts.ai.service.SocialConnectionService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Publishes a video to YouTube as a Short. Async: the upload is initiated here and the
 * {@code SocialPublishStatusService} reconciler finalizes the job once YouTube finishes processing.
 *
 * <p>When {@code app.youtube.direct-publish-enabled=false} (default) the publisher is in scaffold
 * mode and refuses to publish — it does NOT fake a successful post.
 */
@Component
public class YoutubeVideoPublisher implements VideoPublisher {

    private final AppProperties appProperties;
    private final ObjectMapper objectMapper;
    private final YoutubeApiClient youtubeApiClient;
    private final SocialConnectionService socialConnectionService;

    public YoutubeVideoPublisher(
        AppProperties appProperties,
        ObjectMapper objectMapper,
        YoutubeApiClient youtubeApiClient,
        SocialConnectionService socialConnectionService
    ) {
        this.appProperties = appProperties;
        this.objectMapper = objectMapper;
        this.youtubeApiClient = youtubeApiClient;
        this.socialConnectionService = socialConnectionService;
    }

    @Override
    public String providerKey() {
        return SocialPlatform.YOUTUBE.key();
    }

    @Override
    public PublishResult publish(PublishRequest request) {
        if (!StringUtils.hasText(request.finalVideoUrl())) {
            throw new ExternalServiceException("YouTube publish requires a final video URL");
        }
        if (!appProperties.getYoutube().isDirectPublishEnabled()) {
            throw new ExternalServiceException(
                "YouTube direct publish is disabled (scaffold mode). "
                    + "Enable APP_YOUTUBE_DIRECT_PUBLISH_ENABLED after Google API verification.");
        }

        String accessToken = socialConnectionService
            .findAccessToken(request.userId(), request.channelId(), SocialPlatform.YOUTUBE)
            .orElseThrow(() -> new ExternalServiceException("No active YouTube connection for this channel"));

        String title = StringUtils.hasText(request.topic()) ? request.topic() : "Short";
        String description = StringUtils.hasText(request.captionText()) ? request.captionText() : "";
        SocialPublishInit init = youtubeApiClient.uploadShort(accessToken, request.finalVideoUrl(), title, description);

        Map<String, Object> requestPayload = new LinkedHashMap<>();
        requestPayload.put("provider", providerKey());
        requestPayload.put("platform", request.platform());
        requestPayload.put("jobId", request.jobId());
        requestPayload.put("channelId", request.channelId());
        requestPayload.put("targetAccountId", request.targetAccountId());

        return new PublishResult(
            providerKey(),
            init.publishId(),
            "youtube_upload_initialized",
            toJson(requestPayload),
            init.rawResponseJson(),
            request.targetAccountId(),
            true
        );
    }

    private String toJson(Map<String, Object> payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException ex) {
            return "{}";
        }
    }
}
