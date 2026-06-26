package com.autoshorts.ai.publish;

import com.autoshorts.ai.client.InstagramApiClient;
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
 * Publishes a video to Instagram as a Reel. Async two-step: this creates the media container; the
 * {@code SocialPublishStatusService} reconciler waits for it to finish processing, then calls
 * {@code media_publish} and finalizes the job.
 *
 * <p>When {@code app.instagram.direct-publish-enabled=false} (default) the publisher is in scaffold
 * mode and refuses to publish.
 */
@Component
public class InstagramVideoPublisher implements VideoPublisher {

    private final AppProperties appProperties;
    private final ObjectMapper objectMapper;
    private final InstagramApiClient instagramApiClient;
    private final SocialConnectionService socialConnectionService;

    public InstagramVideoPublisher(
        AppProperties appProperties,
        ObjectMapper objectMapper,
        InstagramApiClient instagramApiClient,
        SocialConnectionService socialConnectionService
    ) {
        this.appProperties = appProperties;
        this.objectMapper = objectMapper;
        this.instagramApiClient = instagramApiClient;
        this.socialConnectionService = socialConnectionService;
    }

    @Override
    public String providerKey() {
        return SocialPlatform.INSTAGRAM.key();
    }

    @Override
    public PublishResult publish(PublishRequest request) {
        if (!StringUtils.hasText(request.finalVideoUrl())) {
            throw new ExternalServiceException("Instagram publish requires a final video URL");
        }
        if (!appProperties.getInstagram().isDirectPublishEnabled()) {
            throw new ExternalServiceException(
                "Instagram direct publish is disabled (scaffold mode). "
                    + "Enable APP_INSTAGRAM_DIRECT_PUBLISH_ENABLED after Meta app review.");
        }
        if (!StringUtils.hasText(request.targetAccountId())) {
            throw new ExternalServiceException("Instagram publish requires a connected IG business account id");
        }

        String accessToken = socialConnectionService
            .findAccessToken(request.userId(), request.channelId(), SocialPlatform.INSTAGRAM)
            .orElseThrow(() -> new ExternalServiceException("No active Instagram connection for this channel"));

        SocialPublishInit init = instagramApiClient.createReelContainer(
            accessToken, request.targetAccountId(), request.finalVideoUrl(), request.captionText());

        Map<String, Object> requestPayload = new LinkedHashMap<>();
        requestPayload.put("provider", providerKey());
        requestPayload.put("platform", request.platform());
        requestPayload.put("jobId", request.jobId());
        requestPayload.put("channelId", request.channelId());
        requestPayload.put("igUserId", request.targetAccountId());

        // publishId carries the container id; the reconciler polls it then calls media_publish.
        return new PublishResult(
            providerKey(),
            init.publishId(),
            "instagram_container_created",
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
