package com.autoshorts.ai.client.impl;

import com.autoshorts.ai.client.TikTokApiClient;
import com.autoshorts.ai.client.model.TikTokCreatorInfo;
import com.autoshorts.ai.client.model.TikTokPublishInit;
import com.autoshorts.ai.client.model.TikTokPublishStatus;
import com.autoshorts.ai.client.model.TikTokTokenResponse;
import com.autoshorts.ai.config.AppProperties;
import com.autoshorts.ai.exception.ExternalServiceException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Real TikTok OAuth + Content Posting API client.
 *
 * @see <a href="https://developers.tiktok.com/doc/content-posting-api-get-started/">Content Posting API</a>
 */
@Component
public class TikTokApiClientImpl implements TikTokApiClient {

    private final WebClient webClient;
    private final AppProperties appProperties;
    private final ObjectMapper objectMapper;

    public TikTokApiClientImpl(WebClient.Builder webClientBuilder, AppProperties appProperties, ObjectMapper objectMapper) {
        this.appProperties = appProperties;
        this.objectMapper = objectMapper;
        this.webClient = webClientBuilder
            .baseUrl(trimTrailingSlash(appProperties.getTiktok().getApiBaseUrl()))
            .build();
    }

    @Override
    public TikTokTokenResponse exchangeAuthorizationCode(String code) {
        AppProperties.TikTok cfg = requireCredentials();
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("client_key", cfg.getClientKey());
        form.add("client_secret", cfg.getClientSecret());
        form.add("code", code);
        form.add("grant_type", "authorization_code");
        form.add("redirect_uri", cfg.getRedirectUri());
        return parseTokenResponse(postForm("/v2/oauth/token/", form));
    }

    @Override
    public TikTokTokenResponse refreshAccessToken(String refreshToken) {
        AppProperties.TikTok cfg = requireCredentials();
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("client_key", cfg.getClientKey());
        form.add("client_secret", cfg.getClientSecret());
        form.add("grant_type", "refresh_token");
        form.add("refresh_token", refreshToken);
        return parseTokenResponse(postForm("/v2/oauth/token/", form));
    }

    @Override
    public TikTokCreatorInfo queryCreatorInfo(String accessToken) {
        JsonNode root = postJson("/v2/post/publish/creator_info/query/", accessToken, Map.of());
        JsonNode data = requireOkData(root, "creator info query");
        List<String> privacyOptions = new ArrayList<>();
        data.path("privacy_level_options").forEach(node -> privacyOptions.add(node.asText()));
        return new TikTokCreatorInfo(
            data.path("creator_username").asText(null),
            data.path("creator_nickname").asText(null),
            privacyOptions,
            data.path("max_video_post_duration_sec").asInt(0),
            data.path("comment_disabled").asBoolean(false),
            data.path("duet_disabled").asBoolean(false),
            data.path("stitch_disabled").asBoolean(false)
        );
    }

    @Override
    public TikTokPublishInit initDirectPostFromUrl(String accessToken, String videoUrl, String caption, String privacyLevel) {
        if (!StringUtils.hasText(videoUrl)) {
            throw new ExternalServiceException("TikTok publish requires a publicly reachable video URL");
        }
        Map<String, Object> postInfo = new java.util.LinkedHashMap<>();
        postInfo.put("title", caption == null ? "" : caption);
        postInfo.put("privacy_level", StringUtils.hasText(privacyLevel) ? privacyLevel : "SELF_ONLY");
        postInfo.put("disable_comment", false);
        postInfo.put("disable_duet", false);
        postInfo.put("disable_stitch", false);

        Map<String, Object> sourceInfo = Map.of(
            "source", "PULL_FROM_URL",
            "video_url", videoUrl
        );
        Map<String, Object> body = Map.of("post_info", postInfo, "source_info", sourceInfo);

        JsonNode root = postJson("/v2/post/publish/video/init/", accessToken, body);
        JsonNode data = requireOkData(root, "direct post init");
        String publishId = data.path("publish_id").asText(null);
        if (!StringUtils.hasText(publishId)) {
            throw new ExternalServiceException("TikTok direct post init returned no publish_id");
        }
        return new TikTokPublishInit(publishId, root.toString());
    }

    @Override
    public TikTokPublishStatus fetchPostStatus(String accessToken, String publishId) {
        JsonNode root = postJson("/v2/post/publish/status/fetch/", accessToken, Map.of("publish_id", publishId));
        JsonNode data = requireOkData(root, "post status fetch");
        String publicPostId = null;
        JsonNode postIds = data.path("publicaly_available_post_id");
        if (postIds.isArray() && !postIds.isEmpty()) {
            publicPostId = postIds.get(0).asText(null);
        }
        return new TikTokPublishStatus(
            data.path("status").asText(null),
            publicPostId,
            data.path("fail_reason").asText(null),
            root.toString()
        );
    }

    private TikTokTokenResponse parseTokenResponse(JsonNode root) {
        // OAuth token endpoint returns fields at the top level (not under "data").
        String error = root.path("error").asText(null);
        if (StringUtils.hasText(error) && !"ok".equalsIgnoreCase(error)) {
            throw new ExternalServiceException(
                "TikTok token request failed: error=%s description=%s".formatted(error, root.path("error_description").asText("")));
        }
        String accessToken = root.path("access_token").asText(null);
        if (!StringUtils.hasText(accessToken)) {
            throw new ExternalServiceException("TikTok token response missing access_token: " + truncate(root.toString(), 400));
        }
        Instant now = Instant.now();
        List<String> scopes = StringUtils.hasText(root.path("scope").asText(null))
            ? Arrays.stream(root.path("scope").asText().split("[,\\s]+")).filter(StringUtils::hasText).toList()
            : List.of();
        return new TikTokTokenResponse(
            accessToken,
            root.path("refresh_token").asText(null),
            root.path("open_id").asText(null),
            scopes,
            instantFromExpiresIn(now, root.path("expires_in").asLong(0)),
            instantFromExpiresIn(now, root.path("refresh_expires_in").asLong(0))
        );
    }

    private Instant instantFromExpiresIn(Instant now, long seconds) {
        return seconds > 0 ? now.plusSeconds(seconds) : null;
    }

    private JsonNode postForm(String path, MultiValueMap<String, String> form) {
        String raw = webClient.post()
            .uri(path)
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .body(BodyInserters.fromFormData(form))
            .retrieve()
            .onStatus(status -> status.isError(), TikTokApiClientImpl::mapError)
            .bodyToMono(String.class)
            .block();
        return readTree(raw);
    }

    private JsonNode postJson(String path, String accessToken, Object body) {
        if (!StringUtils.hasText(accessToken)) {
            throw new ExternalServiceException("TikTok API call requires an access token");
        }
        String raw = webClient.post()
            .uri(path)
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
            .contentType(MediaType.parseMediaType("application/json; charset=UTF-8"))
            .bodyValue(body)
            .retrieve()
            .onStatus(status -> status.isError(), TikTokApiClientImpl::mapError)
            .bodyToMono(String.class)
            .block();
        return readTree(raw);
    }

    private static reactor.core.publisher.Mono<Throwable> mapError(
        org.springframework.web.reactive.function.client.ClientResponse response) {
        return response.bodyToMono(String.class)
            .defaultIfEmpty("")
            .map(body -> new ExternalServiceException(
                "TikTok API request failed: status=%s body=%s".formatted(response.statusCode(), truncate(body, 800))));
    }

    /** TikTok wraps payloads in {@code {data:{...}, error:{code:"ok"|...}}}; validate and return data. */
    private JsonNode requireOkData(JsonNode root, String operation) {
        JsonNode error = root.path("error");
        String code = error.path("code").asText("ok");
        if (StringUtils.hasText(code) && !"ok".equalsIgnoreCase(code)) {
            throw new ExternalServiceException(
                "TikTok %s failed: code=%s message=%s logId=%s".formatted(
                    operation, code, error.path("message").asText(""), error.path("log_id").asText("")));
        }
        return root.path("data");
    }

    private JsonNode readTree(String raw) {
        try {
            return objectMapper.readTree(raw == null ? "{}" : raw);
        } catch (Exception ex) {
            throw new ExternalServiceException("Failed to parse TikTok response: " + truncate(raw, 400), ex);
        }
    }

    private AppProperties.TikTok requireCredentials() {
        AppProperties.TikTok cfg = appProperties.getTiktok();
        if (!StringUtils.hasText(cfg.getClientKey()) || !StringUtils.hasText(cfg.getClientSecret())) {
            throw new ExternalServiceException(
                "TikTok client credentials are not configured. Set TIKTOK_CLIENT_KEY and TIKTOK_CLIENT_SECRET.");
        }
        return cfg;
    }

    private static String trimTrailingSlash(String value) {
        String result = StringUtils.hasText(value) ? value.trim() : "https://open.tiktokapis.com";
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
