package com.autoshorts.ai.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "news_items")
public class NewsItem {

    @Id
    private UUID id;

    @Column(name = "news_source_id", nullable = false)
    private UUID newsSourceId;

    @Column(name = "external_guid", nullable = false, length = 512)
    private String externalGuid;

    @Column(nullable = false, length = 1000)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String link;

    @Column(columnDefinition = "TEXT")
    private String summary;

    @Column(name = "published_at")
    private Instant publishedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private NewsItemStatus status = NewsItemStatus.NEW;

    @Column(name = "topic_idea_id")
    private UUID topicIdeaId;

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
        if (status == null) {
            status = NewsItemStatus.NEW;
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

    public UUID getNewsSourceId() {
        return newsSourceId;
    }

    public void setNewsSourceId(UUID newsSourceId) {
        this.newsSourceId = newsSourceId;
    }

    public String getExternalGuid() {
        return externalGuid;
    }

    public void setExternalGuid(String externalGuid) {
        this.externalGuid = externalGuid;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getLink() {
        return link;
    }

    public void setLink(String link) {
        this.link = link;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public Instant getPublishedAt() {
        return publishedAt;
    }

    public void setPublishedAt(Instant publishedAt) {
        this.publishedAt = publishedAt;
    }

    public NewsItemStatus getStatus() {
        return status;
    }

    public void setStatus(NewsItemStatus status) {
        this.status = status;
    }

    public UUID getTopicIdeaId() {
        return topicIdeaId;
    }

    public void setTopicIdeaId(UUID topicIdeaId) {
        this.topicIdeaId = topicIdeaId;
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
