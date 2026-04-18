package com.autoshorts.ai.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public class TopicIdeaCreateRequest {

    @NotBlank(message = "topic is required")
    @Size(max = 500, message = "topic must be <= 500 characters")
    private String topic;

    @Size(max = 100, message = "contentStyle must be <= 100 characters")
    private String contentStyle;

    @Min(value = 0, message = "priority must be >= 0")
    @Max(value = 1000, message = "priority must be <= 1000")
    private Integer priority = 0;

    @Size(max = 100, message = "source must be <= 100 characters")
    private String source;

    @Size(max = 20, message = "tags must contain at most 20 entries")
    private List<@Size(max = 50, message = "tag must be <= 50 characters") String> tags;

    private UUID channelId;

    private Instant scheduledFor;

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public String getContentStyle() {
        return contentStyle;
    }

    public void setContentStyle(String contentStyle) {
        this.contentStyle = contentStyle;
    }

    public Integer getPriority() {
        return priority;
    }

    public void setPriority(Integer priority) {
        this.priority = priority;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }

    public UUID getChannelId() {
        return channelId;
    }

    public void setChannelId(UUID channelId) {
        this.channelId = channelId;
    }

    public Instant getScheduledFor() {
        return scheduledFor;
    }

    public void setScheduledFor(Instant scheduledFor) {
        this.scheduledFor = scheduledFor;
    }
}
