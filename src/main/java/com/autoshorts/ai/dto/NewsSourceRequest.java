package com.autoshorts.ai.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public class NewsSourceRequest {

    @NotBlank(message = "name is required")
    @Size(max = 200)
    private String name;

    @NotBlank(message = "feedUrl is required")
    @Size(max = 2000)
    private String feedUrl;

    private UUID channelId;

    private boolean enabled = true;

    private boolean autoPublish = false;

    @Min(1)
    @Max(10080)
    private int fetchIntervalMinutes = 60;

    @Min(1)
    @Max(50)
    private int maxItemsPerFetch = 5;

    @Size(max = 100)
    private String contentStyle;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getFeedUrl() {
        return feedUrl;
    }

    public void setFeedUrl(String feedUrl) {
        this.feedUrl = feedUrl;
    }

    public UUID getChannelId() {
        return channelId;
    }

    public void setChannelId(UUID channelId) {
        this.channelId = channelId;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isAutoPublish() {
        return autoPublish;
    }

    public void setAutoPublish(boolean autoPublish) {
        this.autoPublish = autoPublish;
    }

    public int getFetchIntervalMinutes() {
        return fetchIntervalMinutes;
    }

    public void setFetchIntervalMinutes(int fetchIntervalMinutes) {
        this.fetchIntervalMinutes = fetchIntervalMinutes;
    }

    public int getMaxItemsPerFetch() {
        return maxItemsPerFetch;
    }

    public void setMaxItemsPerFetch(int maxItemsPerFetch) {
        this.maxItemsPerFetch = maxItemsPerFetch;
    }

    public String getContentStyle() {
        return contentStyle;
    }

    public void setContentStyle(String contentStyle) {
        this.contentStyle = contentStyle;
    }
}
