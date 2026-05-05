package com.autoshorts.ai.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public class GenerateVideoRequest {

    @NotBlank(message = "topic is required")
    @Size(max = 500, message = "topic must be <= 500 characters")
    private String topic;

    @Size(max = 100, message = "style must be <= 100 characters")
    private String style = "motivation";

    @Size(max = 100, message = "contentStyle must be <= 100 characters")
    private String contentStyle;

    @Size(max = 200, message = "voiceId must be <= 200 characters")
    private String voiceId;

    private UUID channelId;

    private UUID characterProfileId;

    private UUID characterCampaignId;

    @Size(max = 200, message = "storyAngle must be <= 200 characters")
    private String storyAngle;

    @Size(max = 64, message = "productPlacementMode must be <= 64 characters")
    private String productPlacementMode;

    @Size(max = 64, message = "adDisclosureMode must be <= 64 characters")
    private String adDisclosureMode;

    @Min(value = 1, message = "sceneCountTarget must be >= 1")
    @Max(value = 20, message = "sceneCountTarget must be <= 20")
    private Integer sceneCountTarget;

    @Size(max = 64, message = "characterConsistencyMode must be <= 64 characters")
    private String characterConsistencyMode;

    @Size(max = 80, message = "niche must be <= 80 characters")
    private String niche = "affiliate";

    @Size(max = 50, message = "platform must be <= 50 characters")
    private String platform = "tiktok";

    @Size(max = 80, message = "subtitleStyle must be <= 80 characters")
    private String subtitleStyle = "tiktok-bold";

    @Size(max = 80, message = "visualMode must be <= 80 characters")
    private String visualMode = "ai-scenes";

    @Size(max = 80, message = "voiceProvider must be <= 80 characters")
    private String voiceProvider;

    @Size(max = 120, message = "voicePersona must be <= 120 characters")
    private String voicePersona = "energetic-creator";

    @Size(max = 80, message = "qualityPreset must be <= 80 characters")
    private String qualityPreset = "viral-faceless";

    @NotNull(message = "durationSeconds is required")
    @Min(value = 10, message = "durationSeconds must be >= 10")
    @Max(value = 120, message = "durationSeconds must be <= 120")
    private Integer durationSeconds = 30;

    @Min(value = 1, message = "variantCount must be >= 1")
    @Max(value = 10, message = "variantCount must be <= 10")
    private Integer variantCount = 1;

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

    public UUID getChannelId() {
        return channelId;
    }

    public void setChannelId(UUID channelId) {
        this.channelId = channelId;
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

    public String getContentStyle() {
        return contentStyle;
    }

    public void setContentStyle(String contentStyle) {
        this.contentStyle = contentStyle;
    }

    public Integer getDurationSeconds() {
        return durationSeconds;
    }

    public void setDurationSeconds(Integer durationSeconds) {
        this.durationSeconds = durationSeconds;
    }

    public Integer getVariantCount() {
        return variantCount;
    }

    public void setVariantCount(Integer variantCount) {
        this.variantCount = variantCount;
    }
}
