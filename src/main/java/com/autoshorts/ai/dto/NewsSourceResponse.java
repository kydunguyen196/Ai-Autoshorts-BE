package com.autoshorts.ai.dto;

import com.autoshorts.ai.entity.NewsSource;

import java.time.Instant;
import java.util.UUID;

public class NewsSourceResponse {

    private UUID id;
    private UUID channelId;
    private String name;
    private String feedUrl;
    private boolean enabled;
    private boolean autoPublish;
    private int fetchIntervalMinutes;
    private int maxItemsPerFetch;
    private String contentStyle;
    private Instant lastFetchedAt;
    private String lastStatus;
    private String lastError;
    private Instant createdAt;
    private Instant updatedAt;

    public static NewsSourceResponse from(NewsSource s) {
        NewsSourceResponse r = new NewsSourceResponse();
        r.id = s.getId();
        r.channelId = s.getChannelId();
        r.name = s.getName();
        r.feedUrl = s.getFeedUrl();
        r.enabled = s.isEnabled();
        r.autoPublish = s.isAutoPublish();
        r.fetchIntervalMinutes = s.getFetchIntervalMinutes();
        r.maxItemsPerFetch = s.getMaxItemsPerFetch();
        r.contentStyle = s.getContentStyle();
        r.lastFetchedAt = s.getLastFetchedAt();
        r.lastStatus = s.getLastStatus();
        r.lastError = s.getLastError();
        r.createdAt = s.getCreatedAt();
        r.updatedAt = s.getUpdatedAt();
        return r;
    }

    public UUID getId() {
        return id;
    }

    public UUID getChannelId() {
        return channelId;
    }

    public String getName() {
        return name;
    }

    public String getFeedUrl() {
        return feedUrl;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public boolean isAutoPublish() {
        return autoPublish;
    }

    public int getFetchIntervalMinutes() {
        return fetchIntervalMinutes;
    }

    public int getMaxItemsPerFetch() {
        return maxItemsPerFetch;
    }

    public String getContentStyle() {
        return contentStyle;
    }

    public Instant getLastFetchedAt() {
        return lastFetchedAt;
    }

    public String getLastStatus() {
        return lastStatus;
    }

    public String getLastError() {
        return lastError;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
