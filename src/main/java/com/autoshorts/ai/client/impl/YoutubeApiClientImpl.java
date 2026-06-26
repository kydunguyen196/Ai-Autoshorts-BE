package com.autoshorts.ai.client.impl;

import com.autoshorts.ai.client.YoutubeApiClient;
import com.autoshorts.ai.client.model.SocialPublishInit;
import com.autoshorts.ai.client.model.SocialPublishStatus;
import com.autoshorts.ai.config.AppProperties;
import com.autoshorts.ai.exception.ExternalServiceException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * YouTube Data API v3 client for uploading Shorts.
 *
 * <p>Only active when {@code app.youtube.direct-publish-enabled=true}; the scaffold path needs no
 * real API. A Short is just a regular {@code videos.insert} upload of a vertical, <=60s clip
 * (the {@code #Shorts} hint goes in the title/description).
 *
 * @see <a href="https://developers.google.com/youtube/v3/docs/videos/insert">videos.insert</a>
 */
@Component
@CircuitBreaker(name = "social-publish")
public class YoutubeApiClientImpl implements YoutubeApiClient {

    private static final Logger log = LoggerFactory.getLogger(YoutubeApiClientImpl.class);

    private final WebClient.Builder webClientBuilder;
    private final AppProperties appProperties;
    private final ObjectMapper objectMapper;

    public YoutubeApiClientImpl(
        WebClient.Builder webClientBuilder,
        AppProperties appProperties,
        ObjectMapper objectMapper
    ) {
        this.webClientBuilder = webClientBuilder;
        this.appProperties = appProperties;
        this.objectMapper = objectMapper;
    }

    @Override
    public SocialPublishInit uploadShort(String accessToken, String videoUrl, String title, String description) {
        requireToken(accessToken);
        if (!StringUtils.hasText(videoUrl) || videoUrl.startsWith("file:")) {
            throw new ExternalServiceException(
                "YouTube upload requires a publicly reachable HTTPS video URL (S3/CDN), got: " + videoUrl);
        }

        byte[] videoBytes = downloadVideo(videoUrl);

        Map<String, Object> snippet = new LinkedHashMap<>();
        snippet.put("title", trimTitle(title));
        snippet.put("description", description == null ? "" : description);
        snippet.put("categoryId", "22"); // People & Blogs
        Map<String, Object> statusBlock = Map.of("privacyStatus", "private", "selfDeclaredMadeForKids", false);
        Map<String, Object> metadata = Map.of("snippet", snippet, "status", statusBlock);

        MultipartBodyBuilder parts = new MultipartBodyBuilder();
        parts.part("metadata", metadata, MediaType.APPLICATION_JSON);
        parts.part("media", videoBytes).contentType(MediaType.valueOf("video/*"));

        JsonNode root = uploadClient().post()
            .uri(uriBuilder -> uriBuilder
                .path("/upload/youtube/v3/videos")
                .queryParam("uploadType", "multipart")
                .queryParam("part", "snippet,status")
                .build())
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
            .contentType(MediaType.MULTIPART_FORM_DATA)
            .body(BodyInserters.fromMultipartData(parts.build()))
            .retrieve()
            .onStatus(status -> status.isError(), YoutubeApiClientImpl::mapError)
            .bodyToMono(String.class)
            .map(this::readTree)
            .block(timeout());

        String videoId = root == null ? null : root.path("id").asText(null);
        if (!StringUtils.hasText(videoId)) {
            throw new ExternalServiceException("YouTube upload returned no video id: " + truncate(String.valueOf(root), 400));
        }
        log.info("event=youtube_upload_initiated videoId={}", videoId);
        return new SocialPublishInit(videoId, root.toString());
    }

    @Override
    public SocialPublishStatus fetchUploadStatus(String accessToken, String videoId) {
        requireToken(accessToken);
        JsonNode root = apiClient().get()
            .uri(uriBuilder -> uriBuilder
                .path("/youtube/v3/videos")
                .queryParam("part", "status,processingDetails")
                .queryParam("id", videoId)
                .build())
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
            .retrieve()
            .onStatus(status -> status.isError(), YoutubeApiClientImpl::mapError)
            .bodyToMono(String.class)
            .map(this::readTree)
            .block(timeout());

        JsonNode items = root == null ? null : root.path("items");
        if (items == null || !items.isArray() || items.isEmpty()) {
            return new SocialPublishStatus("FAILED", null, "video not found", String.valueOf(root));
        }
        JsonNode item = items.get(0);
        String uploadStatus = item.path("status").path("uploadStatus").asText("");
        String processingStatus = item.path("processingDetails").path("processingStatus").asText("");
        String failureReason = item.path("status").path("failureReason").asText(null);
        // YouTube: uploadStatus=processed + processingStatus=succeeded => ready.
        String normalized = "processed".equalsIgnoreCase(uploadStatus) ? "PROCESSED"
            : "failed".equalsIgnoreCase(uploadStatus) ? "FAILED"
            : StringUtils.hasText(processingStatus) ? processingStatus.toUpperCase()
            : uploadStatus.toUpperCase();
        return new SocialPublishStatus(normalized, item.path("id").asText(null), failureReason, root.toString());
    }

    private byte[] downloadVideo(String videoUrl) {
        byte[] bytes = webClientBuilder.build().get()
            .uri(videoUrl)
            .retrieve()
            .onStatus(status -> status.isError(), YoutubeApiClientImpl::mapError)
            .bodyToMono(byte[].class)
            .block(timeout());
        if (bytes == null || bytes.length == 0) {
            throw new ExternalServiceException("Downloaded an empty video from " + videoUrl);
        }
        return bytes;
    }

    private WebClient uploadClient() {
        return webClientBuilder.baseUrl(baseUrl()).build();
    }

    private WebClient apiClient() {
        return webClientBuilder.baseUrl(baseUrl()).build();
    }

    private String baseUrl() {
        String url = appProperties.getYoutube().getApiBaseUrl();
        return StringUtils.hasText(url) ? trimTrailingSlash(url) : "https://www.googleapis.com";
    }

    private Duration timeout() {
        long minutes = Math.max(1, appProperties.getYoutube().getStatusTimeoutMinutes());
        return Duration.ofMinutes(Math.min(minutes, 10));
    }

    private void requireToken(String accessToken) {
        if (!StringUtils.hasText(accessToken)) {
            throw new ExternalServiceException("YouTube API call requires an access token");
        }
    }

    private String trimTitle(String title) {
        String value = StringUtils.hasText(title) ? title.trim() : "Short";
        return value.length() <= 100 ? value : value.substring(0, 100);
    }

    private static reactor.core.publisher.Mono<Throwable> mapError(ClientResponse response) {
        return response.bodyToMono(String.class)
            .defaultIfEmpty("")
            .map(body -> new ExternalServiceException(
                "YouTube API request failed: status=%s body=%s".formatted(response.statusCode(), truncate(body, 800))));
    }

    private JsonNode readTree(String raw) {
        try {
            return objectMapper.readTree(raw == null ? "{}" : raw);
        } catch (Exception ex) {
            throw new ExternalServiceException("Failed to parse YouTube response: " + truncate(raw, 400), ex);
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

    // Retained to document the response shape the reconciler relies on.
    @SuppressWarnings("unused")
    private static final List<String> READY_STATES = List.of("PROCESSED", "SUCCEEDED");
}
