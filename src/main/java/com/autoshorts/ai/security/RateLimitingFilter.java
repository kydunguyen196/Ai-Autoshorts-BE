package com.autoshorts.ai.security;

import com.autoshorts.ai.config.AppProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Per-client fixed-window rate limiter. Uses Redis (shared across instances) when reachable so
 * limits hold in a multi-node deployment, and falls back to an in-process counter when Redis is
 * unavailable (e.g. tests / single-node dev) — failing open rather than blocking traffic.
 */
@Component
public class RateLimitingFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RateLimitingFilter.class);
    private static final long WINDOW_MILLIS = 60_000L;

    private final AppProperties appProperties;
    private final ObjectMapper objectMapper;
    private final ObjectProvider<StringRedisTemplate> redisTemplateProvider;
    private final boolean trustForwardedFor;
    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();
    private final AtomicBoolean redisWarningLogged = new AtomicBoolean(false);

    public RateLimitingFilter(
        AppProperties appProperties,
        ObjectMapper objectMapper,
        ObjectProvider<StringRedisTemplate> redisTemplateProvider,
        @Value("${app.rate-limit.trust-forwarded-for:false}") boolean trustForwardedFor
    ) {
        this.appProperties = appProperties;
        this.objectMapper = objectMapper;
        this.redisTemplateProvider = redisTemplateProvider;
        this.trustForwardedFor = trustForwardedFor;
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

        String group = rateLimitGroup(path);
        String key = clientKey(request) + ":" + group;
        long count = incrementCount(key);

        response.setHeader("X-RateLimit-Limit", String.valueOf(limit));
        response.setHeader("X-RateLimit-Remaining", String.valueOf(Math.max(0, limit - count)));

        if (count <= limit) {
            filterChain.doFilter(request, response);
            return;
        }

        response.setStatus(429);
        response.setHeader("Retry-After", "60");
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getWriter(), Map.of(
            "timestamp", Instant.now().toString(),
            "status", 429,
            "error", "Too Many Requests",
            "message", "Rate limit exceeded. Try again later.",
            "path", path
        ));
    }

    /** Returns the current request count in this window, using Redis when available. */
    private long incrementCount(String key) {
        StringRedisTemplate redis = redisTemplateProvider.getIfAvailable();
        if (redis != null) {
            try {
                long window = System.currentTimeMillis() / WINDOW_MILLIS;
                String redisKey = "ratelimit:" + key + ":" + window;
                Long count = redis.opsForValue().increment(redisKey);
                if (count != null) {
                    if (count == 1L) {
                        redis.expire(redisKey, Duration.ofMillis(WINDOW_MILLIS));
                    }
                    return count;
                }
            } catch (RuntimeException ex) {
                if (redisWarningLogged.compareAndSet(false, true)) {
                    log.warn("event=rate_limit_redis_unavailable fallback=in_memory message={}", ex.getMessage());
                }
            }
        }
        return incrementInMemory(key);
    }

    private long incrementInMemory(String key) {
        long now = System.currentTimeMillis();
        Bucket bucket = buckets.compute(key, (ignored, existing) -> {
            if (existing == null || now >= existing.windowStartedAtMillis + WINDOW_MILLIS) {
                return new Bucket(now, 1);
            }
            existing.count++;
            return existing;
        });
        return bucket.count;
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

    /**
     * Resolves the client identity. X-Forwarded-For is only trusted when explicitly enabled
     * (app.rate-limit.trust-forwarded-for=true, i.e. behind a trusted proxy/CDN); otherwise the
     * header is ignored to prevent spoofing the rate-limit bucket.
     */
    private String clientKey(HttpServletRequest request) {
        if (trustForwardedFor) {
            String forwardedFor = request.getHeader("X-Forwarded-For");
            if (forwardedFor != null && !forwardedFor.isBlank()) {
                return forwardedFor.split(",")[0].trim();
            }
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
