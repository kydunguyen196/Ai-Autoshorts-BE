package com.autoshorts.ai.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "news_sources")
public class NewsSource {

    @Id
    private UUID id;

    @Column(name = "owner_user_id", nullable = false)
    private UUID ownerUserId;

    @Column(name = "channel_id", nullable = false)
    private UUID channelId;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(name = "feed_url", nullable = false, columnDefinition = "TEXT")
    private String feedUrl;

    @Column(nullable = false)
    private boolean enabled = true;

    @Column(name = "auto_publish", nullable = false)
    private boolean autoPublish = false;

    @Column(name = "fetch_interval_minutes", nullable = false)
    private int fetchIntervalMinutes = 60;

    @Column(name = "max_items_per_fetch", nullable = false)
    private int maxItemsPerFetch = 5;

    @Column(name = "content_style", length = 100)
    private String contentStyle;

    @Column(name = "last_fetched_at")
    private Instant lastFetchedAt;

    @Column(name = "last_status", length = 32)
    private String lastStatus;

    @Column(name = "last_error", columnDefinition = "TEXT")
    private String lastError;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    public void onCreate() {
        Instant now = Instant.now();
        if (id == null) {
            id = UUID.randomUUID();
        }
        if (createdAt == null) {
            createdAt = now;
        }
        updatedAt = now;
    }

    @PreUpdate
    public void onUpdate() {
        updatedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getOwnerUserId() {
        return ownerUserId;
    }

    public void setOwnerUserId(UUID ownerUserId) {
        this.ownerUserId = ownerUserId;
    }

    public UUID getChannelId() {
        return channelId;
    }

    public void setChannelId(UUID channelId) {
        this.channelId = channelId;
    }

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

    public Instant getLastFetchedAt() {
        return lastFetchedAt;
    }

    public void setLastFetchedAt(Instant lastFetchedAt) {
        this.lastFetchedAt = lastFetchedAt;
    }

    public String getLastStatus() {
        return lastStatus;
    }

    public void setLastStatus(String lastStatus) {
        this.lastStatus = lastStatus;
    }

    public String getLastError() {
        return lastError;
    }

    public void setLastError(String lastError) {
        this.lastError = lastError;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
