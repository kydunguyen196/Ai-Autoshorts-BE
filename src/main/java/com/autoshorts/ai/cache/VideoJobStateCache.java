package com.autoshorts.ai.cache;

import com.autoshorts.ai.dto.VideoJobResponse;

import java.util.Optional;
import java.util.UUID;

public interface VideoJobStateCache {

    Optional<VideoJobResponse> get(UUID jobId);

    void put(VideoJobResponse response);
}
