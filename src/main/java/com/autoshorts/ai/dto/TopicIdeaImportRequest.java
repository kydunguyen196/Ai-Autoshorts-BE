package com.autoshorts.ai.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

public class TopicIdeaImportRequest {

    @NotEmpty(message = "topics must not be empty")
    @Valid
    private List<TopicIdeaCreateRequest> topics;

    @Size(max = 100, message = "defaultSource must be <= 100 characters")
    private String defaultSource;

    private UUID defaultChannelId;

    public List<TopicIdeaCreateRequest> getTopics() {
        return topics;
    }

    public void setTopics(List<TopicIdeaCreateRequest> topics) {
        this.topics = topics;
    }

    public String getDefaultSource() {
        return defaultSource;
    }

    public void setDefaultSource(String defaultSource) {
        this.defaultSource = defaultSource;
    }

    public UUID getDefaultChannelId() {
        return defaultChannelId;
    }

    public void setDefaultChannelId(UUID defaultChannelId) {
        this.defaultChannelId = defaultChannelId;
    }
}
