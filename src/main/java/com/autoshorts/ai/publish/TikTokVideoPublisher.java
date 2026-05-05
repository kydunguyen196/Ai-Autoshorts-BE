package com.autoshorts.ai.publish;

import com.autoshorts.ai.config.AppProperties;
import com.autoshorts.ai.exception.ExternalServiceException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class TikTokVideoPublisher implements VideoPublisher {

    private final AppProperties appProperties;
    private final ObjectMapper objectMapper;

    public TikTokVideoPublisher(AppProperties appProperties, ObjectMapper objectMapper) {
        this.appProperties = appProperties;
        this.objectMapper = objectMapper;
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
        if (!Boolean.getBoolean("autoshorts.tiktok.direct.enabled")) {
            throw new ExternalServiceException("TikTok direct publish is not enabled yet. Use the exported download URL while the TikTok API integration remains in scaffold mode.");
        }

        Map<String, Object> creatorInfoQuery = new LinkedHashMap<>();
        creatorInfoQuery.put("operation", "creator.info.query");
        creatorInfoQuery.put("scopes", appProperties.getTiktok().getScopes());

        Map<String, Object> directPostInit = new LinkedHashMap<>();
        directPostInit.put("operation", "post.direct.init");
        directPostInit.put("caption", request.captionText());
        directPostInit.put("privacyLevel", "PUBLIC_TO_EVERYONE");
        directPostInit.put("adDisclosureMode", "DECLARED_BY_BRAND");

        Map<String, Object> uploadByUrlModel = new LinkedHashMap<>();
        uploadByUrlModel.put("operation", "post.upload.pull_from_url");
        uploadByUrlModel.put("sourceUrl", request.finalVideoUrl());

        Map<String, Object> fetchStatusModel = new LinkedHashMap<>();
        fetchStatusModel.put("operation", "post.status.query");
        fetchStatusModel.put("pollingHint", "Use publish external id to query final processing status.");

        Map<String, Object> requestPayload = new LinkedHashMap<>();
        requestPayload.put("provider", providerKey());
        requestPayload.put("platform", request.platform());
        requestPayload.put("jobId", request.jobId());
        requestPayload.put("channelId", request.channelId());
        requestPayload.put("targetAccountId", request.targetAccountId());
        requestPayload.put("apiBaseUrl", appProperties.getTiktok().getApiBaseUrl());
        requestPayload.put("creatorInfoQuery", creatorInfoQuery);
        requestPayload.put("directPostInit", directPostInit);
        requestPayload.put("uploadByUrl", uploadByUrlModel);
        requestPayload.put("fetchStatus", fetchStatusModel);

        String externalId = "tiktok-scaffold-" + request.jobId();

        Map<String, Object> responsePayload = new LinkedHashMap<>();
        responsePayload.put("accepted", true);
        responsePayload.put("externalId", externalId);
        responsePayload.put("mode", "SCAFFOLD");
        responsePayload.put("timestamp", Instant.now());
        responsePayload.put("errorMappingHint", "Map TikTok API error codes to publish_failure_reason/details.");

        return new PublishResult(
            providerKey(),
            externalId,
            "tiktok_scaffold_initialized",
            toJson(requestPayload),
            toJson(responsePayload),
            request.targetAccountId()
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
