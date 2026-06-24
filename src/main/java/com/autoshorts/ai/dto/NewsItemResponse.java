package com.autoshorts.ai.dto;

import com.autoshorts.ai.entity.NewsItem;

import java.time.Instant;
import java.util.UUID;

public class NewsItemResponse {

    private UUID id;
    private UUID newsSourceId;
    private String title;
    private String link;
    private String summary;
    private Instant publishedAt;
    private String status;
    private UUID topicIdeaId;
    private Instant createdAt;

    public static NewsItemResponse from(NewsItem n) {
        NewsItemResponse r = new NewsItemResponse();
        r.id = n.getId();
        r.newsSourceId = n.getNewsSourceId();
        r.title = n.getTitle();
        r.link = n.getLink();
        r.summary = n.getSummary();
        r.publishedAt = n.getPublishedAt();
        r.status = n.getStatus() != null ? n.getStatus().name() : null;
        r.topicIdeaId = n.getTopicIdeaId();
        r.createdAt = n.getCreatedAt();
        return r;
    }

    public UUID getId() {
        return id;
    }

    public UUID getNewsSourceId() {
        return newsSourceId;
    }

    public String getTitle() {
        return title;
    }

    public String getLink() {
        return link;
    }

    public String getSummary() {
        return summary;
    }

    public Instant getPublishedAt() {
        return publishedAt;
    }

    public String getStatus() {
        return status;
    }

    public UUID getTopicIdeaId() {
        return topicIdeaId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
