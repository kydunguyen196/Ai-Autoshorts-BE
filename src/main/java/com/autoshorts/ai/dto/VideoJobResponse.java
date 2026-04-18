package com.autoshorts.ai.dto;

import com.autoshorts.ai.entity.JobStatus;
import com.autoshorts.ai.entity.GenerationStep;
import com.autoshorts.ai.entity.ContentGenerationMode;
import com.autoshorts.ai.entity.PublishStatus;
import com.autoshorts.ai.entity.AudioGenerationMode;
import com.autoshorts.ai.entity.ReviewStatus;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public class VideoJobResponse {

    private UUID jobId;
    private UUID channelId;
    private JobStatus status;
    private String topic;
    private String style;
    private String voiceId;
    private Integer durationSeconds;
    private String scriptText;
    private String hookText;
    private String ctaText;
    private String captionText;
    private List<String> hashtags;
    private String sceneBreakdownJson;
    private String resolvedStyle;
    private UUID promptTemplateId;
    private ContentGenerationMode contentGenerationMode;
    private String contentVariantKey;
    private String hookStrategy;
    private String ctaStrategy;
    private String structureStrategy;
    private Integer hookStrengthScore;
    private Integer engagementScore;
    private String engagementTagsJson;
    private UUID generationBatchId;
    private UUID generationGroupId;
    private UUID characterProfileId;
    private UUID characterCampaignId;
    private String storyAngle;
    private String productPlacementMode;
    private String adDisclosureMode;
    private Integer sceneCountTarget;
    private String characterConsistencyMode;
    private Integer variantIndex;
    private Integer variantCount;
    private Integer rankingScore;
    private Boolean isTopCandidate;
    private Integer topCandidateRank;
    private ReviewStatus reviewStatus;
    private Instant reviewedAt;
    private UUID reviewedBy;
    private String rejectionReason;
    private Boolean selectedForPublish;
    private PublishStatus publishStatus;
    private Instant scheduledPublishAt;
    private String publishPlatform;
    private Instant publishReadyAt;
    private Instant publishRequestedAt;
    private Instant publishStartedAt;
    private Instant publishedAt;
    private Integer publishAttemptCount;
    private String publishProvider;
    private String publishExternalId;
    private String publishTargetAccountId;
    private String publishRequestPayloadJson;
    private String publishResponsePayloadJson;
    private String publishFailureReason;
    private String publishFailureDetails;
    private Instant publishLastErrorAt;
    private Instant publishLastStatusCheckAt;
    private String audioUrl;
    private AudioGenerationMode audioGenerationMode;
    private String audioProvider;
    private String audioVoiceId;
    private String audioModelId;
    private String audioOutputFormat;
    private Long audioProviderRequestDurationMs;
    private String audioFailureReason;
    private String audioFailureDetails;
    private String subtitleUrl;
    private String finalVideoUrl;
    private String errorMessage;
    private GenerationStep currentStep;
    private String stepErrorDetails;
    private Integer attemptCount;
    private Instant startedAt;
    private Instant completedAt;
    private Instant lastErrorAt;
    private Instant createdAt;
    private Instant updatedAt;

    public UUID getJobId() {
        return jobId;
    }

    public void setJobId(UUID jobId) {
        this.jobId = jobId;
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

    public String getStyle() {
        return style;
    }

    public void setStyle(String style) {
        this.style = style;
    }

    public String getVoiceId() {
        return voiceId;
    }

    public void setVoiceId(String voiceId) {
        this.voiceId = voiceId;
    }

    public Integer getDurationSeconds() {
        return durationSeconds;
    }

    public void setDurationSeconds(Integer durationSeconds) {
        this.durationSeconds = durationSeconds;
    }

    public UUID getChannelId() {
        return channelId;
    }

    public void setChannelId(UUID channelId) {
        this.channelId = channelId;
    }

    public String getScriptText() {
        return scriptText;
    }

    public void setScriptText(String scriptText) {
        this.scriptText = scriptText;
    }

    public String getAudioUrl() {
        return audioUrl;
    }

    public void setAudioUrl(String audioUrl) {
        this.audioUrl = audioUrl;
    }

    public String getHookText() {
        return hookText;
    }

    public void setHookText(String hookText) {
        this.hookText = hookText;
    }

    public String getCtaText() {
        return ctaText;
    }

    public void setCtaText(String ctaText) {
        this.ctaText = ctaText;
    }

    public String getCaptionText() {
        return captionText;
    }

    public void setCaptionText(String captionText) {
        this.captionText = captionText;
    }

    public List<String> getHashtags() {
        return hashtags;
    }

    public void setHashtags(List<String> hashtags) {
        this.hashtags = hashtags;
    }

    public String getSceneBreakdownJson() {
        return sceneBreakdownJson;
    }

    public void setSceneBreakdownJson(String sceneBreakdownJson) {
        this.sceneBreakdownJson = sceneBreakdownJson;
    }

    public String getResolvedStyle() {
        return resolvedStyle;
    }

    public void setResolvedStyle(String resolvedStyle) {
        this.resolvedStyle = resolvedStyle;
    }

    public UUID getPromptTemplateId() {
        return promptTemplateId;
    }

    public void setPromptTemplateId(UUID promptTemplateId) {
        this.promptTemplateId = promptTemplateId;
    }

    public ContentGenerationMode getContentGenerationMode() {
        return contentGenerationMode;
    }

    public void setContentGenerationMode(ContentGenerationMode contentGenerationMode) {
        this.contentGenerationMode = contentGenerationMode;
    }

    public String getContentVariantKey() {
        return contentVariantKey;
    }

    public void setContentVariantKey(String contentVariantKey) {
        this.contentVariantKey = contentVariantKey;
    }

    public String getHookStrategy() {
        return hookStrategy;
    }

    public void setHookStrategy(String hookStrategy) {
        this.hookStrategy = hookStrategy;
    }

    public String getCtaStrategy() {
        return ctaStrategy;
    }

    public void setCtaStrategy(String ctaStrategy) {
        this.ctaStrategy = ctaStrategy;
    }

    public String getStructureStrategy() {
        return structureStrategy;
    }

    public void setStructureStrategy(String structureStrategy) {
        this.structureStrategy = structureStrategy;
    }

    public Integer getHookStrengthScore() {
        return hookStrengthScore;
    }

    public void setHookStrengthScore(Integer hookStrengthScore) {
        this.hookStrengthScore = hookStrengthScore;
    }

    public Integer getEngagementScore() {
        return engagementScore;
    }

    public void setEngagementScore(Integer engagementScore) {
        this.engagementScore = engagementScore;
    }

    public String getEngagementTagsJson() {
        return engagementTagsJson;
    }

    public void setEngagementTagsJson(String engagementTagsJson) {
        this.engagementTagsJson = engagementTagsJson;
    }

    public UUID getGenerationBatchId() {
        return generationBatchId;
    }

    public void setGenerationBatchId(UUID generationBatchId) {
        this.generationBatchId = generationBatchId;
    }

    public UUID getGenerationGroupId() {
        return generationGroupId;
    }

    public void setGenerationGroupId(UUID generationGroupId) {
        this.generationGroupId = generationGroupId;
    }

    public UUID getCharacterProfileId() {
        return characterProfileId;
    }

    public void setCharacterProfileId(UUID characterProfileId) {
        this.characterProfileId = characterProfileId;
    }

    public UUID getCharacterCampaignId() {
        return characterCampaignId;
    }

    public void setCharacterCampaignId(UUID characterCampaignId) {
        this.characterCampaignId = characterCampaignId;
    }

    public String getStoryAngle() {
        return storyAngle;
    }

    public void setStoryAngle(String storyAngle) {
        this.storyAngle = storyAngle;
    }

    public String getProductPlacementMode() {
        return productPlacementMode;
    }

    public void setProductPlacementMode(String productPlacementMode) {
        this.productPlacementMode = productPlacementMode;
    }

    public String getAdDisclosureMode() {
        return adDisclosureMode;
    }

    public void setAdDisclosureMode(String adDisclosureMode) {
        this.adDisclosureMode = adDisclosureMode;
    }

    public Integer getSceneCountTarget() {
        return sceneCountTarget;
    }

    public void setSceneCountTarget(Integer sceneCountTarget) {
        this.sceneCountTarget = sceneCountTarget;
    }

    public String getCharacterConsistencyMode() {
        return characterConsistencyMode;
    }

    public void setCharacterConsistencyMode(String characterConsistencyMode) {
        this.characterConsistencyMode = characterConsistencyMode;
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

    public Integer getRankingScore() {
        return rankingScore;
    }

    public void setRankingScore(Integer rankingScore) {
        this.rankingScore = rankingScore;
    }

    public Boolean getIsTopCandidate() {
        return isTopCandidate;
    }

    public void setIsTopCandidate(Boolean isTopCandidate) {
        this.isTopCandidate = isTopCandidate;
    }

    public Integer getTopCandidateRank() {
        return topCandidateRank;
    }

    public void setTopCandidateRank(Integer topCandidateRank) {
        this.topCandidateRank = topCandidateRank;
    }

    public ReviewStatus getReviewStatus() {
        return reviewStatus;
    }

    public void setReviewStatus(ReviewStatus reviewStatus) {
        this.reviewStatus = reviewStatus;
    }

    public Instant getReviewedAt() {
        return reviewedAt;
    }

    public void setReviewedAt(Instant reviewedAt) {
        this.reviewedAt = reviewedAt;
    }

    public UUID getReviewedBy() {
        return reviewedBy;
    }

    public void setReviewedBy(UUID reviewedBy) {
        this.reviewedBy = reviewedBy;
    }

    public String getRejectionReason() {
        return rejectionReason;
    }

    public void setRejectionReason(String rejectionReason) {
        this.rejectionReason = rejectionReason;
    }

    public Boolean getSelectedForPublish() {
        return selectedForPublish;
    }

    public void setSelectedForPublish(Boolean selectedForPublish) {
        this.selectedForPublish = selectedForPublish;
    }

    public PublishStatus getPublishStatus() {
        return publishStatus;
    }

    public void setPublishStatus(PublishStatus publishStatus) {
        this.publishStatus = publishStatus;
    }

    public Instant getScheduledPublishAt() {
        return scheduledPublishAt;
    }

    public void setScheduledPublishAt(Instant scheduledPublishAt) {
        this.scheduledPublishAt = scheduledPublishAt;
    }

    public String getPublishPlatform() {
        return publishPlatform;
    }

    public void setPublishPlatform(String publishPlatform) {
        this.publishPlatform = publishPlatform;
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

    public String getSubtitleUrl() {
        return subtitleUrl;
    }

    public void setSubtitleUrl(String subtitleUrl) {
        this.subtitleUrl = subtitleUrl;
    }

    public String getFinalVideoUrl() {
        return finalVideoUrl;
    }

    public void setFinalVideoUrl(String finalVideoUrl) {
        this.finalVideoUrl = finalVideoUrl;
    }

    public AudioGenerationMode getAudioGenerationMode() {
        return audioGenerationMode;
    }

    public void setAudioGenerationMode(AudioGenerationMode audioGenerationMode) {
        this.audioGenerationMode = audioGenerationMode;
    }

    public String getAudioProvider() {
        return audioProvider;
    }

    public void setAudioProvider(String audioProvider) {
        this.audioProvider = audioProvider;
    }

    public String getAudioVoiceId() {
        return audioVoiceId;
    }

    public void setAudioVoiceId(String audioVoiceId) {
        this.audioVoiceId = audioVoiceId;
    }

    public String getAudioModelId() {
        return audioModelId;
    }

    public void setAudioModelId(String audioModelId) {
        this.audioModelId = audioModelId;
    }

    public String getAudioOutputFormat() {
        return audioOutputFormat;
    }

    public void setAudioOutputFormat(String audioOutputFormat) {
        this.audioOutputFormat = audioOutputFormat;
    }

    public Long getAudioProviderRequestDurationMs() {
        return audioProviderRequestDurationMs;
    }

    public void setAudioProviderRequestDurationMs(Long audioProviderRequestDurationMs) {
        this.audioProviderRequestDurationMs = audioProviderRequestDurationMs;
    }

    public String getAudioFailureReason() {
        return audioFailureReason;
    }

    public void setAudioFailureReason(String audioFailureReason) {
        this.audioFailureReason = audioFailureReason;
    }

    public String getAudioFailureDetails() {
        return audioFailureDetails;
    }

    public void setAudioFailureDetails(String audioFailureDetails) {
        this.audioFailureDetails = audioFailureDetails;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public GenerationStep getCurrentStep() {
        return currentStep;
    }

    public void setCurrentStep(GenerationStep currentStep) {
        this.currentStep = currentStep;
    }

    public String getStepErrorDetails() {
        return stepErrorDetails;
    }

    public void setStepErrorDetails(String stepErrorDetails) {
        this.stepErrorDetails = stepErrorDetails;
    }

    public Integer getAttemptCount() {
        return attemptCount;
    }

    public void setAttemptCount(Integer attemptCount) {
        this.attemptCount = attemptCount;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(Instant startedAt) {
        this.startedAt = startedAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(Instant completedAt) {
        this.completedAt = completedAt;
    }

    public Instant getLastErrorAt() {
        return lastErrorAt;
    }

    public void setLastErrorAt(Instant lastErrorAt) {
        this.lastErrorAt = lastErrorAt;
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
