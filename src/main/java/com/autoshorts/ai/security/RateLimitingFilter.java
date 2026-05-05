package com.autoshorts.ai.security;

import com.autoshorts.ai.config.AppProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class RateLimitingFilter extends OncePerRequestFilter {

    private static final long WINDOW_MILLIS = 60_000L;

    private final AppProperties appProperties;
    private final ObjectMapper objectMapper;
    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    public RateLimitingFilter(AppProperties appProperties, ObjectMapper objectMapper) {
        this.appProperties = appProperties;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(
        HttpServletRequest request,
        HttpServletResponse response,
        FilterChain filterChain
    ) throws ServletException, IOException {
        if (!appProperties.getRateLimit().isEnabled()) {
            filterChain.doFilter(request, response);
            return;
        }

        String path = request.getRequestURI();
        int limit = resolveLimit(path);
        if (limit <= 0) {
            filterChain.doFilter(request, response);
            return;
        }

        long now = System.currentTimeMillis();
        String key = clientKey(request) + ":" + rateLimitGroup(path);
        Bucket bucket = buckets.compute(key, (ignored, existing) -> {
            if (existing == null || now >= existing.windowStartedAtMillis + WINDOW_MILLIS) {
                return new Bucket(now, 1);
            }
            existing.count++;
            return existing;
        });

        long retryAfterSeconds = Math.max(1L, ((bucket.windowStartedAtMillis + WINDOW_MILLIS) - now + 999L) / 1000L);
        response.setHeader("X-RateLimit-Limit", String.valueOf(limit));
        response.setHeader("X-RateLimit-Remaining", String.valueOf(Math.max(0, limit - bucket.count)));

        if (bucket.count <= limit) {
            filterChain.doFilter(request, response);
            return;
        }

        response.setStatus(429);
        response.setHeader("Retry-After", String.valueOf(retryAfterSeconds));
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getWriter(), Map.of(
            "timestamp", Instant.now().toString(),
            "status", 429,
            "error", "Too Many Requests",
            "message", "Rate limit exceeded. Try again later.",
            "path", path
        ));
    }

    private int resolveLimit(String path) {
        if (path.startsWith("/api/auth/")) {
            return appProperties.getRateLimit().getAuthPerMinute();
        }
        if (path.equals("/api/videos/generate") || path.equals("/api/videos/batch-generate")) {
            return appProperties.getRateLimit().getGeneratePerMinute();
        }
        return appProperties.getRateLimit().getDefaultPerMinute();
    }

    private String rateLimitGroup(String path) {
        if (path.startsWith("/api/auth/")) {
            return "auth";
        }
        if (path.equals("/api/videos/generate") || path.equals("/api/videos/batch-generate")) {
            return "generate";
        }
        return "default";
    }

    private String clientKey(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private static class Bucket {
        private final long windowStartedAtMillis;
        private int count;

        private Bucket(long windowStartedAtMillis, int count) {
            this.windowStartedAtMillis = windowStartedAtMillis;
            this.count = count;
        }
    }
}
