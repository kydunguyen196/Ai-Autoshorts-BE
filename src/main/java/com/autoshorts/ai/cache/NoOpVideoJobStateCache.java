package com.autoshorts.ai.cache;

import com.autoshorts.ai.dto.VideoJobResponse;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
@ConditionalOnMissingBean(VideoJobStateCache.class)
public class NoOpVideoJobStateCache implements VideoJobStateCache {

    @Override
    public Optional<VideoJobResponse> get(UUID jobId) {
        return Optional.empty();
    }

    @Override
    public void put(VideoJobResponse response) {
        // no-op
    }
}
