package com.autoshorts.ai.cache;

import com.autoshorts.ai.config.AppProperties;
import com.autoshorts.ai.dto.VideoJobResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

@Component
@ConditionalOnProperty(name = "app.redis.cache-enabled", havingValue = "true", matchIfMissing = true)
public class RedisVideoJobStateCache implements VideoJobStateCache {

    private static final Logger log = LoggerFactory.getLogger(RedisVideoJobStateCache.class);
    private static final String KEY_PREFIX = "autoshorts:job:";

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final AppProperties appProperties;

    public RedisVideoJobStateCache(StringRedisTemplate redisTemplate, ObjectMapper objectMapper, AppProperties appProperties) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.appProperties = appProperties;
    }

    @Override
    public Optional<VideoJobResponse> get(UUID jobId) {
        try {
            String value = redisTemplate.opsForValue().get(key(jobId));
            if (value == null) {
                return Optional.empty();
            }
            return Optional.of(objectMapper.readValue(value, VideoJobResponse.class));
        } catch (Exception ex) {
            log.debug("event=redis_cache_read_failed jobId={} message={}", jobId, ex.getMessage());
            return Optional.empty();
        }
    }

    @Override
    public void put(VideoJobResponse response) {
        if (response == null || response.getJobId() == null) {
            return;
        }
        try {
            String payload = objectMapper.writeValueAsString(response);
            redisTemplate.opsForValue().set(
                key(response.getJobId()),
                payload,
                Duration.ofSeconds(appProperties.getRedis().getCacheTtlSeconds())
            );
        } catch (JsonProcessingException ex) {
            log.debug("event=redis_cache_serialization_failed jobId={} message={}", response.getJobId(), ex.getMessage());
        } catch (Exception ex) {
            log.debug("event=redis_cache_write_failed jobId={} message={}", response.getJobId(), ex.getMessage());
        }
    }

    private String key(UUID jobId) {
        return KEY_PREFIX + jobId;
    }
}
