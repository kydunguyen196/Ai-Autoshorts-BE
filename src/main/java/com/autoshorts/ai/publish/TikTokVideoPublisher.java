package com.autoshorts.ai.publish;

import com.autoshorts.ai.client.TikTokApiClient;
import com.autoshorts.ai.client.model.TikTokCreatorInfo;
import com.autoshorts.ai.client.model.TikTokPublishInit;
import com.autoshorts.ai.config.AppProperties;
import com.autoshorts.ai.exception.ExternalServiceException;
import com.autoshorts.ai.service.TikTokOAuthService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class TikTokVideoPublisher implements VideoPublisher {

    private final AppProperties appProperties;
    private final ObjectMapper objectMapper;
    private final TikTokApiClient tikTokApiClient;
    private final TikTokOAuthService tikTokOAuthService;

    public TikTokVideoPublisher(
        AppProperties appProperties,
        ObjectMapper objectMapper,
        TikTokApiClient tikTokApiClient,
        TikTokOAuthService tikTokOAuthService
    ) {
        this.appProperties = appProperties;
        this.objectMapper = objectMapper;
        this.tikTokApiClient = tikTokApiClient;
        this.tikTokOAuthService = tikTokOAuthService;
    }

    @Override
    public String providerKey() {
        return "tiktok";
    }

    @Override
    public PublishResult publish(PublishRequest request) {
        if (!StringUtils.hasText(request.finalVideoUrl())) {
            throw new ExternalServiceException("TikTok publish requires a final video URL");
        }
        if (!appProperties.getTiktok().isDirectPublishEnabled()) {
            // Scaffold mode: a real TikTok connection exists but direct publish is not enabled.
            // Do NOT fake a successful post — fail clearly so the job is marked PUBLISH_FAILED.
            throw new ExternalServiceException(
                "TikTok direct publish is disabled (scaffold mode). "
                    + "Enable TIKTOK_DIRECT_PUBLISH_ENABLED after the TikTok app audit before publishing.");
        }
        return directPublish(request);
    }

    /** Real TikTok Content Posting API flow: validate creator, then init a PULL_FROM_URL post. */
    private PublishResult directPublish(PublishRequest request) {
        if (request.finalVideoUrl().startsWith("file:")) {
            throw new ExternalServiceException(
                "TikTok cannot pull a local file:// URL. The video must be served from a public HTTPS URL (S3/CDN).");
        }
        String accessToken = tikTokOAuthService.getValidAccessToken(request.userId(), request.channelId());

        TikTokCreatorInfo creatorInfo = tikTokApiClient.queryCreatorInfo(accessToken);
        String privacyLevel = resolvePrivacyLevel(creatorInfo.privacyLevelOptions());

        TikTokPublishInit init = tikTokApiClient.initDirectPostFromUrl(
            accessToken,
            request.finalVideoUrl(),
            request.captionText(),
            privacyLevel
        );

        Map<String, Object> requestPayload = new LinkedHashMap<>();
        requestPayload.put("provider", providerKey());
        requestPayload.put("platform", request.platform());
        requestPayload.put("jobId", request.jobId());
        requestPayload.put("channelId", request.channelId());
        requestPayload.put("targetAccountId", request.targetAccountId());
        requestPayload.put("privacyLevel", privacyLevel);
        requestPayload.put("source", "PULL_FROM_URL");

        // publishId is the handle the async status reconciler will poll via fetchPostStatus(...).
        // pendingAsync=true keeps the job in PUBLISHING until the reconciler confirms completion.
        return new PublishResult(
            providerKey(),
            init.publishId(),
            "tiktok_publish_initialized",
            toJson(requestPayload),
            init.rawResponseJson(),
            request.targetAccountId(),
            true
        );
    }

    /**
     * Prefer the most private option that is available. Unaudited TikTok apps may only post
     * SELF_ONLY; once audited, the creator's allowed options will include public levels.
     */
    private String resolvePrivacyLevel(List<String> options) {
        if (options == null || options.isEmpty()) {
            return "SELF_ONLY";
        }
        if (options.contains("SELF_ONLY")) {
            return "SELF_ONLY";
        }
        return options.get(0);
    }

    private String toJson(Map<String, Object> payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException ex) {
            return "{}";
        }
    }
}
