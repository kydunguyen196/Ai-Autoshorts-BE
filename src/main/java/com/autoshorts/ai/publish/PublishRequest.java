package com.autoshorts.ai.publish;

import java.util.UUID;

public record PublishRequest(
    UUID jobId,
    UUID userId,
    UUID channelId,
    String topic,
    String platform,
    String finalVideoUrl,
    String captionText,
    String targetAccountId
) {
}
