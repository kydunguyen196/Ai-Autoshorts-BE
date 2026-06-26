package com.autoshorts.ai.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "video_jobs")
public class VideoJob {

    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "channel_id", nullable = false)
    private UUID channelId;

    @Column(nullable = false, length = 500)
    private String topic;

    @Column(nullable = false, length = 100)
    private String style;

    @Column(name = "voice_id", length = 200)
    private String voiceId;

    @Column(name = "duration_seconds", nullable = false)
    private Integer durationSeconds;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private JobStatus status;

    @Column(name = "script_text", columnDefinition = "TEXT")
    private String scriptText;

    @Column(name = "hook_text", columnDefinition = "TEXT")
    private String hookText;

    @Column(name = "cta_text", columnDefinition = "TEXT")
    private String ctaText;

    @Column(name = "caption_text", columnDefinition = "TEXT")
    private String captionText;

    @Column(name = "hashtags", columnDefinition = "TEXT")
    private String hashtags;

    @Column(name = "scene_breakdown_json", columnDefinition = "TEXT")
    private String sceneBreakdownJson;

    @Column(name = "scene_assets_json", columnDefinition = "TEXT")
    private String sceneAssetsJson;

    @Column(name = "resolved_style", length = 100)
    private String resolvedStyle;

    @Column(name = "prompt_template_id")
    private UUID promptTemplateId;

    @Enumerated(EnumType.STRING)
    @Column(name = "content_generation_mode", length = 32)
    private ContentGenerationMode contentGenerationMode;

    @Column(name = "content_variant_key", length = 160)
    private String contentVariantKey;

    @Column(name = "hook_strategy", length = 100)
    private String hookStrategy;

    @Column(name = "cta_strategy", length = 100)
    private String ctaStrategy;

    @Column(name = "structure_strategy", length = 100)
    private String structureStrategy;

    @Column(name = "hook_strength_score")
    private Integer hookStrengthScore;

    @Column(name = "engagement_score")
    private Integer engagementScore;

    @Column(name = "engagement_tags_json", columnDefinition = "TEXT")
    private String engagementTagsJson;

    @Column(name = "generation_batch_id")
    private UUID generationBatchId;

    @Column(name = "generation_group_id")
    private UUID generationGroupId;

    @Column(name = "character_profile_id")
    private UUID characterProfileId;

    @Column(name = "character_campaign_id")
    private UUID characterCampaignId;

    @Column(name = "story_angle", length = 200)
    private String storyAngle;

    @Column(name = "product_placement_mode", length = 64)
    private String productPlacementMode;

    @Column(name = "ad_disclosure_mode", length = 64)
    private String adDisclosureMode;

    @Column(name = "scene_count_target")
    private Integer sceneCountTarget;

    @Column(name = "character_consistency_mode", length = 64)
    private String characterConsistencyMode;

    @Column(name = "niche", length = 80)
    private String niche;

    @Column(name = "platform", length = 50)
    private String platform;

    @Column(name = "subtitle_style", length = 80)
    private String subtitleStyle;

    @Column(name = "visual_mode", length = 80)
    private String visualMode;

    @Column(name = "voice_provider", length = 80)
    private String voiceProvider;

    @Column(name = "voice_persona", length = 120)
    private String voicePersona;

    @Column(name = "quality_preset", length = 80)
    private String qualityPreset;

    @Column(name = "variant_index")
    private Integer variantIndex;

    @Column(name = "variant_count")
    private Integer variantCount;

    @Column(name = "ranking_score")
    private Integer rankingScore;

    @Column(name = "is_top_candidate", nullable = false)
    private Boolean topCandidate = false;

    @Column(name = "top_candidate_rank")
    private Integer topCandidateRank;

    @Enumerated(EnumType.STRING)
    @Column(name = "review_status", length = 32, nullable = false)
    private ReviewStatus reviewStatus = ReviewStatus.DRAFT;

    @Column(name = "reviewed_at")
    private Instant reviewedAt;

    @Column(name = "reviewed_by")
    private UUID reviewedBy;

    @Column(name = "rejection_reason", columnDefinition = "TEXT")
    private String rejectionReason;

    @Column(name = "selected_for_publish", nullable = false)
    private Boolean selectedForPublish = false;

    @Column(name = "auto_publish", nullable = false)
    private Boolean autoPublish = false;

    @Column(name = "review_before_render", nullable = false)
    private Boolean reviewBeforeRender = false;

    @Column(name = "render_approved", nullable = false)
    private Boolean renderApproved = false;

    @Column(name = "template_id")
    private UUID templateId;

    @Enumerated(EnumType.STRING)
    @Column(name = "publish_status", length = 32, nullable = false)
    private PublishStatus publishStatus = PublishStatus.NOT_PUBLISHED;

    @Column(name = "scheduled_publish_at")
    private Instant scheduledPublishAt;

    @Column(name = "publish_platform", length = 50)
    private String publishPlatform;

    @Column(name = "publish_ready_at")
    private Instant publishReadyAt;

    @Column(name = "publish_requested_at")
    private Instant publishRequestedAt;

    @Column(name = "publish_started_at")
    private Instant publishStartedAt;

    @Column(name = "published_at")
    private Instant publishedAt;

    @Column(name = "publish_attempt_count", nullable = false)
    private Integer publishAttemptCount = 0;

    @Column(name = "publish_provider", length = 80)
    private String publishProvider;

    @Column(name = "publish_external_id", length = 255)
    private String publishExternalId;

    @Column(name = "publish_target_account_id", length = 255)
    private String publishTargetAccountId;

    @Column(name = "publish_request_payload_json", columnDefinition = "TEXT")
    private String publishRequestPayloadJson;

    @Column(name = "publish_response_payload_json", columnDefinition = "TEXT")
    private String publishResponsePayloadJson;

    @Column(name = "publish_last_status_check_at")
    private Instant publishLastStatusCheckAt;

    @Column(name = "publish_failure_reason", columnDefinition = "TEXT")
    private String publishFailureReason;

    @Column(name = "publish_failure_details", columnDefinition = "TEXT")
    private String publishFailureDetails;

    @Column(name = "publish_last_error_at")
    private Instant publishLastErrorAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "export_status", length = 32, nullable = false)
    private ExportStatus exportStatus = ExportStatus.NOT_READY;

    @Column(name = "download_url", columnDefinition = "TEXT")
    private String downloadUrl;

    @Column(name = "estimated_cost_credits")
    private Integer estimatedCostCredits;

    @Column(name = "audio_url", columnDefinition = "TEXT")
    private String audioUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "audio_generation_mode", length = 32)
    private AudioGenerationMode audioGenerationMode;

    @Column(name = "audio_provider", length = 80)
    private String audioProvider;

    @Column(name = "audio_voice_id", length = 200)
    private String audioVoiceId;

    @Column(name = "audio_model_id", length = 120)
    private String audioModelId;

    @Column(name = "audio_output_format", length = 80)
    private String audioOutputFormat;

    @Column(name = "audio_provider_request_duration_ms")
    private Long audioProviderRequestDurationMs;

    @Column(name = "audio_failure_reason", columnDefinition = "TEXT")
    private String audioFailureReason;

    @Column(name = "audio_failure_details", columnDefinition = "TEXT")
    private String audioFailureDetails;

    @Enumerated(EnumType.STRING)
    @Column(name = "visual_generation_mode", length = 32)
    private VisualGenerationMode visualGenerationMode;

    @Column(name = "visual_provider", length = 80)
    private String visualProvider;

    @Column(name = "visual_model_id", length = 120)
    private String visualModelId;

    @Column(name = "visual_failure_reason", columnDefinition = "TEXT")
    private String visualFailureReason;

    @Column(name = "visual_failure_details", columnDefinition = "TEXT")
    private String visualFailureDetails;

    @Column(name = "subtitle_url", columnDefinition = "TEXT")
    private String subtitleUrl;

    @Column(name = "final_video_url", columnDefinition = "TEXT")
    private String finalVideoUrl;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Enumerated(EnumType.STRING)
    @Column(name = "current_step", length = 64)
    private GenerationStep currentStep;

    @Column(name = "step_error_details", columnDefinition = "TEXT")
    private String stepErrorDetails;

    @Version
    @Column(nullable = false)
    private Long version = 0L;

    @Column(name = "attempt_count", nullable = false)
    private Integer attemptCount = 0;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "last_error_at")
    private Instant lastErrorAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    public void onCreate() {
        Instant now = Instant.now();
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

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public UUID getChannelId() {
        return channelId;
    }

    public void setChannelId(UUID channelId) {
        this.channelId = channelId;
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

    public JobStatus getStatus() {
        return status;
    }

    public void setStatus(JobStatus status) {
        this.status = status;
    }

    public String getScriptText() {
        return scriptText;
    }

    public void setScriptText(String scriptText) {
        this.scriptText = scriptText;
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

    public String getHashtags() {
        return hashtags;
    }

    public void setHashtags(String hashtags) {
        this.hashtags = hashtags;
    }

    public String getSceneBreakdownJson() {
        return sceneBreakdownJson;
    }

    public void setSceneBreakdownJson(String sceneBreakdownJson) {
        this.sceneBreakdownJson = sceneBreakdownJson;
    }

    public String getSceneAssetsJson() {
        return sceneAssetsJson;
    }

    public void setSceneAssetsJson(String sceneAssetsJson) {
        this.sceneAssetsJson = sceneAssetsJson;
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

    public String getNiche() {
        return niche;
    }

    public void setNiche(String niche) {
        this.niche = niche;
    }

    public String getPlatform() {
        return platform;
    }

    public void setPlatform(String platform) {
        this.platform = platform;
    }

    public String getSubtitleStyle() {
        return subtitleStyle;
    }

    public void setSubtitleStyle(String subtitleStyle) {
        this.subtitleStyle = subtitleStyle;
    }

    public String getVisualMode() {
        return visualMode;
    }

    public void setVisualMode(String visualMode) {
        this.visualMode = visualMode;
    }

    public String getVoiceProvider() {
        return voiceProvider;
    }

    public void setVoiceProvider(String voiceProvider) {
        this.voiceProvider = voiceProvider;
    }

    public String getVoicePersona() {
        return voicePersona;
    }

    public void setVoicePersona(String voicePersona) {
        this.voicePersona = voicePersona;
    }

    public String getQualityPreset() {
        return qualityPreset;
    }

    public void setQualityPreset(String qualityPreset) {
        this.qualityPreset = qualityPreset;
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

    public Boolean getTopCandidate() {
        return topCandidate;
    }

    public void setTopCandidate(Boolean topCandidate) {
        this.topCandidate = topCandidate;
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

    public Boolean getAutoPublish() {
        return autoPublish;
    }

    public void setAutoPublish(Boolean autoPublish) {
        this.autoPublish = autoPublish;
    }

    public Boolean getReviewBeforeRender() {
        return reviewBeforeRender;
    }

    public void setReviewBeforeRender(Boolean reviewBeforeRender) {
        this.reviewBeforeRender = reviewBeforeRender;
    }

    public Boolean getRenderApproved() {
        return renderApproved;
    }

    public void setRenderApproved(Boolean renderApproved) {
        this.renderApproved = renderApproved;
    }

    public UUID getTemplateId() {
        return templateId;
    }

    public void setTemplateId(UUID templateId) {
        this.templateId = templateId;
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

    public Instant getPublishLastStatusCheckAt() {
        return publishLastStatusCheckAt;
    }

    public void setPublishLastStatusCheckAt(Instant publishLastStatusCheckAt) {
        this.publishLastStatusCheckAt = publishLastStatusCheckAt;
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

    public ExportStatus getExportStatus() {
        return exportStatus;
    }

    public void setExportStatus(ExportStatus exportStatus) {
        this.exportStatus = exportStatus;
    }

    public String getDownloadUrl() {
        return downloadUrl;
    }

    public void setDownloadUrl(String downloadUrl) {
        this.downloadUrl = downloadUrl;
    }

    public Integer getEstimatedCostCredits() {
        return estimatedCostCredits;
    }

    public void setEstimatedCostCredits(Integer estimatedCostCredits) {
        this.estimatedCostCredits = estimatedCostCredits;
    }

    public String getAudioUrl() {
        return audioUrl;
    }

    public void setAudioUrl(String audioUrl) {
        this.audioUrl = audioUrl;
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

    public VisualGenerationMode getVisualGenerationMode() {
        return visualGenerationMode;
    }

    public void setVisualGenerationMode(VisualGenerationMode visualGenerationMode) {
        this.visualGenerationMode = visualGenerationMode;
    }

    public String getVisualProvider() {
        return visualProvider;
    }

    public void setVisualProvider(String visualProvider) {
        this.visualProvider = visualProvider;
    }

    public String getVisualModelId() {
        return visualModelId;
    }

    public void setVisualModelId(String visualModelId) {
        this.visualModelId = visualModelId;
    }

    public String getVisualFailureReason() {
        return visualFailureReason;
    }

    public void setVisualFailureReason(String visualFailureReason) {
        this.visualFailureReason = visualFailureReason;
    }

    public String getVisualFailureDetails() {
        return visualFailureDetails;
    }

    public void setVisualFailureDetails(String visualFailureDetails) {
        this.visualFailureDetails = visualFailureDetails;
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

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
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
