package com.autoshorts.ai.dto;

import com.autoshorts.ai.entity.PublishStatus;
import com.autoshorts.ai.entity.ReviewStatus;

import java.time.Instant;
import java.util.UUID;

public class VideoPublishStatusResponse {

    private UUID jobId;
    private PublishStatus publishStatus;
    private String publishPlatform;
    private String publishProvider;
    private String publishExternalId;
    private String publishTargetAccountId;
    private String publishRequestPayloadJson;
    private String publishResponsePayloadJson;
    private Instant publishReadyAt;
    private Instant publishRequestedAt;
    private Instant publishStartedAt;
    private Instant publishedAt;
    private Integer publishAttemptCount;
    private String publishFailureReason;
    private String publishFailureDetails;
    private Instant publishLastErrorAt;
    private Instant publishLastStatusCheckAt;
    private ReviewStatus reviewStatus;
    private Boolean selectedForPublish;
    private Boolean publishable;
    private String publishReadinessReason;
    private String tiktokConnectionStatus;

    public UUID getJobId() {
        return jobId;
    }

    public void setJobId(UUID jobId) {
        this.jobId = jobId;
    }

    public PublishStatus getPublishStatus() {
        return publishStatus;
    }

    public void setPublishStatus(PublishStatus publishStatus) {
        this.publishStatus = publishStatus;
    }

    public String getPublishPlatform() {
        return publishPlatform;
    }

    public void setPublishPlatform(String publishPlatform) {
        this.publishPlatform = publishPlatform;
    }

    public String getPublishProvider() {
        return publishProvider;
    }

    public void setPublishProvider(String publishProvider) {
        this.publishProvider = publishProvider;
    }

    public String getPublishExternalId() {
        return publishExternalId;
    }

    public void setPublishExternalId(String publishExternalId) {
        this.publishExternalId = publishExternalId;
    }

    public String getPublishTargetAccountId() {
        return publishTargetAccountId;
    }

    public void setPublishTargetAccountId(String publishTargetAccountId) {
        this.publishTargetAccountId = publishTargetAccountId;
    }

    public String getPublishRequestPayloadJson() {
        return publishRequestPayloadJson;
    }

    public void setPublishRequestPayloadJson(String publishRequestPayloadJson) {
        this.publishRequestPayloadJson = publishRequestPayloadJson;
    }

    public String getPublishResponsePayloadJson() {
        return publishResponsePayloadJson;
    }

    public void setPublishResponsePayloadJson(String publishResponsePayloadJson) {
        this.publishResponsePayloadJson = publishResponsePayloadJson;
    }

    public Instant getPublishReadyAt() {
        return publishReadyAt;
    }

    public void setPublishReadyAt(Instant publishReadyAt) {
        this.publishReadyAt = publishReadyAt;
    }

    public Instant getPublishRequestedAt() {
        return publishRequestedAt;
    }

    public void setPublishRequestedAt(Instant publishRequestedAt) {
        this.publishRequestedAt = publishRequestedAt;
    }

    public Instant getPublishStartedAt() {
        return publishStartedAt;
    }

    public void setPublishStartedAt(Instant publishStartedAt) {
        this.publishStartedAt = publishStartedAt;
    }

    public Instant getPublishedAt() {
        return publishedAt;
    }

    public void setPublishedAt(Instant publishedAt) {
        this.publishedAt = publishedAt;
    }

    public Integer getPublishAttemptCount() {
        return publishAttemptCount;
    }

    public void setPublishAttemptCount(Integer publishAttemptCount) {
        this.publishAttemptCount = publishAttemptCount;
    }

    public String getPublishFailureReason() {
        return publishFailureReason;
    }

    public void setPublishFailureReason(String publishFailureReason) {
        this.publishFailureReason = publishFailureReason;
    }

    public String getPublishFailureDetails() {
        return publishFailureDetails;
    }

    public void setPublishFailureDetails(String publishFailureDetails) {
        this.publishFailureDetails = publishFailureDetails;
    }

    public Instant getPublishLastErrorAt() {
        return publishLastErrorAt;
    }

    public void setPublishLastErrorAt(Instant publishLastErrorAt) {
        this.publishLastErrorAt = publishLastErrorAt;
    }

    public Instant getPublishLastStatusCheckAt() {
        return publishLastStatusCheckAt;
    }

    public void setPublishLastStatusCheckAt(Instant publishLastStatusCheckAt) {
        this.publishLastStatusCheckAt = publishLastStatusCheckAt;
    }

    public ReviewStatus getReviewStatus() {
        return reviewStatus;
    }

    public void setReviewStatus(ReviewStatus reviewStatus) {
        this.reviewStatus = reviewStatus;
    }

    public Boolean getSelectedForPublish() {
        return selectedForPublish;
    }

    public void setSelectedForPublish(Boolean selectedForPublish) {
        this.selectedForPublish = selectedForPublish;
    }

    public Boolean getPublishable() {
        return publishable;
    }

    public void setPublishable(Boolean publishable) {
        this.publishable = publishable;
    }

    public String getPublishReadinessReason() {
        return publishReadinessReason;
    }

    public void setPublishReadinessReason(String publishReadinessReason) {
        this.publishReadinessReason = publishReadinessReason;
    }

    public String getTiktokConnectionStatus() {
        return tiktokConnectionStatus;
    }

    public void setTiktokConnectionStatus(String tiktokConnectionStatus) {
        this.tiktokConnectionStatus = tiktokConnectionStatus;
    }
}
