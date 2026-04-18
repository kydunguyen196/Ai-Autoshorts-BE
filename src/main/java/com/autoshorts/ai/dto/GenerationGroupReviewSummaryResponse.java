package com.autoshorts.ai.dto;

import com.autoshorts.ai.entity.ReviewStatus;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public class GenerationGroupReviewSummaryResponse {

    private UUID generationGroupId;
    private Integer totalJobs;
    private UUID selectedJobId;
    private Map<ReviewStatus, Integer> reviewStatusCounts = new LinkedHashMap<>();

    public UUID getGenerationGroupId() {
        return generationGroupId;
    }

    public void setGenerationGroupId(UUID generationGroupId) {
        this.generationGroupId = generationGroupId;
    }

    public Integer getTotalJobs() {
        return totalJobs;
    }

    public void setTotalJobs(Integer totalJobs) {
        this.totalJobs = totalJobs;
    }

    public UUID getSelectedJobId() {
        return selectedJobId;
    }

    public void setSelectedJobId(UUID selectedJobId) {
        this.selectedJobId = selectedJobId;
    }

    public Map<ReviewStatus, Integer> getReviewStatusCounts() {
        return reviewStatusCounts;
    }

    public void setReviewStatusCounts(Map<ReviewStatus, Integer> reviewStatusCounts) {
        this.reviewStatusCounts = reviewStatusCounts;
    }
}
