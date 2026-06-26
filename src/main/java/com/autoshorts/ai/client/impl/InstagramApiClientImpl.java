package com.autoshorts.ai.client.impl;

import com.autoshorts.ai.client.InstagramApiClient;
import com.autoshorts.ai.client.model.SocialPublishInit;
import com.autoshorts.ai.client.model.SocialPublishStatus;
import com.autoshorts.ai.config.AppProperties;
import com.autoshorts.ai.exception.ExternalServiceException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;

/**
 * Instagram Graph API client for publishing Reels via the two-step container flow:
 * {@code POST /{ig-user-id}/media} (media_type=REELS, video_url=...) → poll {@code status_code} →
 * {@code POST /{ig-user-id}/media_publish}.
 *
 * <p>Only active when {@code app.instagram.direct-publish-enabled=true}.
 *
 * @see <a href="https://developers.facebook.com/docs/instagram-api/guides/content-publishing">Content Publishing</a>
 */
@Component
@CircuitBreaker(name = "social-publish")
public class InstagramApiClientImpl implements InstagramApiClient {

    private static final Logger log = LoggerFactory.getLogger(InstagramApiClientImpl.class);

    private final WebClient.Builder webClientBuilder;
    private final AppProperties appProperties;
    private final ObjectMapper objectMapper;

    public InstagramApiClientImpl(
        WebClient.Builder webClientBuilder,
        AppProperties appProperties,
        ObjectMapper objectMapper
    ) {
        this.webClientBuilder = webClientBuilder;
        this.appProperties = appProperties;
        this.objectMapper = objectMapper;
    }

    @Override
    public SocialPublishInit createReelContainer(String accessToken, String igUserId, String videoUrl, String caption) {
        requireArgs(accessToken, igUserId);
        if (!StringUtils.hasText(videoUrl) || videoUrl.startsWith("file:")) {
            throw new ExternalServiceException(
                "Instagram Reels require a publicly reachable HTTPS video URL (S3/CDN), got: " + videoUrl);
        }
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("media_type", "REELS");
        form.add("video_url", videoUrl);
        if (StringUtils.hasText(caption)) {
            form.add("caption", caption);
        }
        form.add("access_token", accessToken);

        JsonNode root = postForm("/" + igUserId + "/media", form);
        String containerId = root.path("id").asText(null);
        if (!StringUtils.hasText(containerId)) {
            throw new ExternalServiceException("Instagram media container create returned no id: " + truncate(root.toString(), 400));
        }
        log.info("event=instagram_container_created containerId={}", containerId);
        return new SocialPublishInit(containerId, root.toString());
    }

    @Override
    public SocialPublishStatus fetchContainerStatus(String accessToken, String containerId) {
        requireArgs(accessToken, containerId);
        JsonNode root = client().get()
            .uri(uriBuilder -> uriBuilder
                .path("/" + containerId)
                .queryParam("fields", "status_code,status")
                .queryParam("access_token", accessToken)
                .build())
            .retrieve()
            .onStatus(status -> status.isError(), InstagramApiClientImpl::mapError)
            .bodyToMono(String.class)
            .map(this::readTree)
            .block(timeout());

        String statusCode = root == null ? null : root.path("status_code").asText(null);
        String detail = root == null ? null : root.path("status").asText(null);
        // status_code: IN_PROGRESS | FINISHED | ERROR | EXPIRED | PUBLISHED
        return new SocialPublishStatus(statusCode, null, detail, String.valueOf(root));
    }

    @Override
    public SocialPublishInit publishContainer(String accessToken, String igUserId, String containerId) {
        requireArgs(accessToken, igUserId);
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("creation_id", containerId);
        form.add("access_token", accessToken);

        JsonNode root = postForm("/" + igUserId + "/media_publish", form);
        String mediaId = root.path("id").asText(null);
        if (!StringUtils.hasText(mediaId)) {
            throw new ExternalServiceException("Instagram media_publish returned no id: " + truncate(root.toString(), 400));
        }
        log.info("event=instagram_reel_published mediaId={}", mediaId);
        return new SocialPublishInit(mediaId, root.toString());
    }

    private JsonNode postForm(String path, MultiValueMap<String, String> form) {
        String raw = client().post()
            .uri(path)
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .body(BodyInserters.fromFormData(form))
            .retrieve()
            .onStatus(status -> status.isError(), InstagramApiClientImpl::mapError)
            .bodyToMono(String.class)
            .block(timeout());
        return readTree(raw);
    }

    private WebClient client() {
        String url = appProperties.getInstagram().getApiBaseUrl();
        String base = StringUtils.hasText(url) ? trimTrailingSlash(url) : "https://graph.facebook.com";
        // Graph API is versioned; default to a recent stable version if the base has no version segment.
        if (!base.matches(".*/v\\d+\\.\\d+$")) {
            base = base + "/v21.0";
        }
        return webClientBuilder.baseUrl(base).build();
    }

    private Duration timeout() {
        long minutes = Math.max(1, appProperties.getInstagram().getStatusTimeoutMinutes());
        return Duration.ofMinutes(Math.min(minutes, 10));
    }

    private void requireArgs(String accessToken, String id) {
        if (!StringUtils.hasText(accessToken)) {
            throw new ExternalServiceException("Instagram API call requires an access token");
        }
        if (!StringUtils.hasText(id)) {
            throw new ExternalServiceException("Instagram API call requires an account/container id");
        }
    }

    private static reactor.core.publisher.Mono<Throwable> mapError(ClientResponse response) {
        return response.bodyToMono(String.class)
            .defaultIfEmpty("")
            .map(body -> new ExternalServiceException(
                "Instagram API request failed: status=%s body=%s".formatted(response.statusCode(), truncate(body, 800))));
    }

    private JsonNode readTree(String raw) {
        try {
            return objectMapper.readTree(raw == null ? "{}" : raw);
        } catch (Exception ex) {
            throw new ExternalServiceException("Failed to parse Instagram response: " + truncate(raw, 400), ex);
        }
    }

    private static String trimTrailingSlash(String value) {
        String result = value.trim();
        while (result.endsWith("/")) {
            result = result.substring(0, result.length() - 1);
        }
        return result;
    }

    private static String truncate(String value, int max) {
        if (value == null) {
            return "";
        }
        return value.length() <= max ? value : value.substring(0, max);
    }
}
