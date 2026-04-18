package com.autoshorts.ai.dto;

import java.time.Instant;
import java.util.List;

public class TopicIdeaImportResponse {

    private int totalRequested;
    private int totalImported;
    private Instant createdAt;
    private List<TopicIdeaResponse> topics;

    public int getTotalRequested() {
        return totalRequested;
    }

    public void setTotalRequested(int totalRequested) {
        this.totalRequested = totalRequested;
    }

    public int getTotalImported() {
        return totalImported;
    }

    public void setTotalImported(int totalImported) {
        this.totalImported = totalImported;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public List<TopicIdeaResponse> getTopics() {
        return topics;
    }

    public void setTopics(List<TopicIdeaResponse> topics) {
        this.topics = topics;
    }
}