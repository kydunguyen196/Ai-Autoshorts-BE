package com.autoshorts.ai.queue;

import java.time.Instant;
import java.util.UUID;

public record VideoGenerationJobMessage(
    UUID jobId,
    String dispatchSource,
    Instant queuedAt
) {
}
