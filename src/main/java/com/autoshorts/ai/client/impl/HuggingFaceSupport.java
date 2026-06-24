package com.autoshorts.ai.client.impl;

import com.autoshorts.ai.config.AppProperties;
import com.autoshorts.ai.exception.ExternalServiceException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

final class HuggingFaceSupport {

    private HuggingFaceSupport() {
    }

    static WebClient webClient(WebClient.Builder builder, AppProperties appProperties) {
        AppProperties.HuggingFace hf = appProperties.getHuggingface();
        String token = trimToNull(hf.getApiToken());
        if (!StringUtils.hasText(token)) {
            throw new ExternalServiceException("Missing Hugging Face API token. Set HUGGINGFACE_API_TOKEN in Saas/.env");
        }
        return builder
            .baseUrl(trimTrailingSlash(hf.getBaseUrl()))
            .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + token)
            .build();
    }

    static Mono<Throwable> errorFromResponse(org.springframework.web.reactive.function.client.ClientResponse response) {
        return response.bodyToMono(String.class)
            .defaultIfEmpty("")
            .map(body -> new ExternalServiceException(
                "Hugging Face request failed: status=%s body=%s".formatted(response.statusCode(), truncate(body, 800))
            ));
    }

    static boolean isError(HttpStatusCode statusCode) {
        return statusCode.isError();
    }

    static String providerModelPath(String provider, String model) {
        String resolvedProvider = StringUtils.hasText(provider) ? provider.trim() : "hf-inference";
        String resolvedModel = trimToNull(model);
        if (!StringUtils.hasText(resolvedModel)) {
            throw new ExternalServiceException("Missing Hugging Face model id");
        }
        return "/%s/models/%s".formatted(resolvedProvider, encodeModelPath(resolvedModel));
    }

    static MediaType mediaTypeForAudioFormat(String format) {
        return switch (normalizeAudioExtension(format)) {
            case "wav" -> MediaType.parseMediaType("audio/wav");
            case "flac" -> MediaType.parseMediaType("audio/flac");
            case "ogg" -> MediaType.parseMediaType("audio/ogg");
            default -> MediaType.parseMediaType("audio/mpeg");
        };
    }

    static String normalizeAudioExtension(String format) {
        String lower = format == null ? "" : format.trim().toLowerCase();
        if (lower.startsWith("wav")) {
            return "wav";
        }
        if (lower.startsWith("flac")) {
            return "flac";
        }
        if (lower.startsWith("ogg") || lower.startsWith("opus")) {
            return "ogg";
        }
        return "mp3";
    }

    static String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private static String encodeModelPath(String model) {
        return URLEncoder.encode(model, StandardCharsets.UTF_8).replace("+", "%20").replace("%2F", "/");
    }

    private static String trimTrailingSlash(String value) {
        String result = StringUtils.hasText(value) ? value.trim() : "https://router.huggingface.co";
        while (result.endsWith("/")) {
            result = result.substring(0, result.length() - 1);
        }
        return result;
    }

    private static String truncate(String value, int max) {
        if (value == null || value.length() <= max) {
            return value == null ? "" : value;
        }
        return value.substring(0, max);
    }
}
