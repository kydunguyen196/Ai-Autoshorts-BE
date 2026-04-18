package com.autoshorts.ai.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public class BatchGenerateResponse {

    private UUID batchId;
    private int totalRequested;
    private int totalVariantsRequested;
    private int totalAccepted;
    private Instant createdAt;
    private List<BatchGenerateJobResult> jobs;

    public UUID getBatchId() {
        return batchId;
    }

    public void setBatchId(UUID batchId) {
        this.batchId = batchId;
    }

    public int getTotalRequested() {
        return totalRequested;
    }

    public void setTotalRequested(int totalRequested) {
        this.totalRequested = totalRequested;
    }

    public int getTotalVariantsRequested() {
        return totalVariantsRequested;
    }

    public void setTotalVariantsRequested(int totalVariantsRequested) {
        this.totalVariantsRequested = totalVariantsRequested;
    }

    public int getTotalAccepted() {
        return totalAccepted;
    }

    public void setTotalAccepted(int totalAccepted) {
        this.totalAccepted = totalAccepted;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public List<BatchGenerateJobResult> getJobs() {
        return jobs;
    }

    public void setJobs(List<BatchGenerateJobResult> jobs) {
        this.jobs = jobs;
    }
}
