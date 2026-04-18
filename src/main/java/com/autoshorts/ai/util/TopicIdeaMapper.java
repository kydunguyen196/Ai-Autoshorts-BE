package com.autoshorts.ai.util;

import com.autoshorts.ai.dto.TopicIdeaResponse;
import com.autoshorts.ai.entity.TopicIdea;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.List;

public final class TopicIdeaMapper {

    private TopicIdeaMapper() {
    }

    public static TopicIdeaResponse toResponse(TopicIdea topicIdea) {
        TopicIdeaResponse response = new TopicIdeaResponse();
        response.setId(topicIdea.getId());
        response.setChannelId(topicIdea.getChannelId());
        response.setTopic(topicIdea.getTopic());
        response.setContentStyle(topicIdea.getContentStyle());
        response.setPriority(topicIdea.getPriority());
        response.setStatus(topicIdea.getStatus());
        response.setSource(topicIdea.getSource());
        response.setTags(parseTags(topicIdea.getTags()));
        response.setScheduledFor(topicIdea.getScheduledFor());
        response.setLastUsedAt(topicIdea.getLastUsedAt());
        response.setCreatedAt(topicIdea.getCreatedAt());
        response.setUpdatedAt(topicIdea.getUpdatedAt());
        return response;
    }

    public static String joinTags(List<String> tags) {
        if (tags == null || tags.isEmpty()) {
            return null;
        }
        return tags.stream()
            .filter(StringUtils::hasText)
            .map(String::trim)
            .map(String::toLowerCase)
            .map(tag -> tag.replaceAll("[^a-z0-9_-]", ""))
            .filter(StringUtils::hasText)
            .distinct()
            .limit(20)
            .reduce((a, b) -> a + "," + b)
            .orElse(null);
    }

    private static List<String> parseTags(String tags) {
        if (!StringUtils.hasText(tags)) {
            return null;
        }
        return Arrays.stream(tags.split(","))
            .map(String::trim)
            .filter(StringUtils::hasText)
            .toList();
    }
}
