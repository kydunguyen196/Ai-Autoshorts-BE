package com.autoshorts.ai.dto;

import com.autoshorts.ai.entity.JobStatus;

import java.util.UUID;

public class BatchGenerateJobResult {

    private UUID jobId;
    private UUID batchId;
    private UUID generationGroupId;
    private Integer variantIndex;
    private Integer variantCount;
    private JobStatus status;
    private String topic;
    private String errorMessage;

    public UUID getJobId() {
        return jobId;
    }

    public void setJobId(UUID jobId) {
        this.jobId = jobId;
    }

    public UUID getBatchId() {
        return batchId;
    }

    public void setBatchId(UUID batchId) {
        this.batchId = batchId;
    }

    public UUID getGenerationGroupId() {
        return generationGroupId;
    }

    public void setGenerationGroupId(UUID generationGroupId) {
        this.generationGroupId = generationGroupId;
    }

    public Integer getVariantIndex() {
        return variantIndex;
    }

    public void setVariantIndex(Integer variantIndex) {
        this.variantIndex = variantIndex;
    }

    public Integer getVariantCount() {
        return variantCount;
    }

    public void setVariantCount(Integer variantCount) {
        this.variantCount = variantCount;
    }

    public JobStatus getStatus() {
        return status;
    }

    public void setStatus(JobStatus status) {
        this.status = status;
    }

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
}
