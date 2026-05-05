package com.autoshorts.ai.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

public class BatchGenerateRequest {

    @NotEmpty(message = "items must not be empty")
    @Size(max = 100, message = "items must contain at most 100 entries")
    @Valid
    private List<BatchGenerateItemRequest> items;

    @Size(max = 100, message = "defaultStyle must be <= 100 characters")
    private String defaultStyle = "motivation";

    @Size(max = 100, message = "defaultContentStyle must be <= 100 characters")
    private String defaultContentStyle;

    @Size(max = 200, message = "defaultVoiceId must be <= 200 characters")
    private String defaultVoiceId;

    private UUID defaultChannelId;

    private UUID defaultCharacterProfileId;

    private UUID defaultCharacterCampaignId;

    @Size(max = 200, message = "defaultStoryAngle must be <= 200 characters")
    private String defaultStoryAngle;

    @Size(max = 64, message = "defaultProductPlacementMode must be <= 64 characters")
    private String defaultProductPlacementMode;

    @Size(max = 64, message = "defaultAdDisclosureMode must be <= 64 characters")
    private String defaultAdDisclosureMode;

    @Min(value = 1, message = "defaultSceneCountTarget must be >= 1")
    @Max(value = 20, message = "defaultSceneCountTarget must be <= 20")
    private Integer defaultSceneCountTarget;

    @Size(max = 64, message = "defaultCharacterConsistencyMode must be <= 64 characters")
    private String defaultCharacterConsistencyMode;

    @Size(max = 80, message = "defaultNiche must be <= 80 characters")
    private String defaultNiche = "affiliate";

    @Size(max = 50, message = "defaultPlatform must be <= 50 characters")
    private String defaultPlatform = "tiktok";

    @Size(max = 80, message = "defaultSubtitleStyle must be <= 80 characters")
    private String defaultSubtitleStyle = "tiktok-bold";

    @Size(max = 80, message = "defaultVisualMode must be <= 80 characters")
    private String defaultVisualMode = "ai-scenes";

    @Size(max = 80, message = "defaultVoiceProvider must be <= 80 characters")
    private String defaultVoiceProvider;

    @Size(max = 120, message = "defaultVoicePersona must be <= 120 characters")
    private String defaultVoicePersona = "energetic-creator";

    @Size(max = 80, message = "defaultQualityPreset must be <= 80 characters")
    private String defaultQualityPreset = "viral-faceless";

    @Min(value = 10, message = "defaultDurationSeconds must be >= 10")
    @Max(value = 120, message = "defaultDurationSeconds must be <= 120")
    private Integer defaultDurationSeconds = 30;

    @Min(value = 1, message = "defaultVariantCount must be >= 1")
    @Max(value = 10, message = "defaultVariantCount must be <= 10")
    private Integer defaultVariantCount = 1;

    public List<BatchGenerateItemRequest> getItems() {
        return items;
    }

    public void setItems(List<BatchGenerateItemRequest> items) {
        this.items = items;
    }

    public String getDefaultStyle() {
        return defaultStyle;
    }

    public void setDefaultStyle(String defaultStyle) {
        this.defaultStyle = defaultStyle;
    }

    public String getDefaultContentStyle() {
        return defaultContentStyle;
    }

    public void setDefaultContentStyle(String defaultContentStyle) {
        this.defaultContentStyle = defaultContentStyle;
    }

    public String getDefaultVoiceId() {
        return defaultVoiceId;
    }

    public void setDefaultVoiceId(String defaultVoiceId) {
        this.defaultVoiceId = defaultVoiceId;
    }

    public UUID getDefaultChannelId() {
        return defaultChannelId;
    }

    public void setDefaultChannelId(UUID defaultChannelId) {
        this.defaultChannelId = defaultChannelId;
    }

    public UUID getDefaultCharacterProfileId() {
        return defaultCharacterProfileId;
    }

    public void setDefaultCharacterProfileId(UUID defaultCharacterProfileId) {
        this.defaultCharacterProfileId = defaultCharacterProfileId;
    }

    public UUID getDefaultCharacterCampaignId() {
        return defaultCharacterCampaignId;
    }

    public void setDefaultCharacterCampaignId(UUID defaultCharacterCampaignId) {
        this.defaultCharacterCampaignId = defaultCharacterCampaignId;
    }

    public String getDefaultStoryAngle() {
        return defaultStoryAngle;
    }

    public void setDefaultStoryAngle(String defaultStoryAngle) {
        this.defaultStoryAngle = defaultStoryAngle;
    }

    public String getDefaultProductPlacementMode() {
        return defaultProductPlacementMode;
    }

    public void setDefaultProductPlacementMode(String defaultProductPlacementMode) {
        this.defaultProductPlacementMode = defaultProductPlacementMode;
    }

    public String getDefaultAdDisclosureMode() {
        return defaultAdDisclosureMode;
    }

    public void setDefaultAdDisclosureMode(String defaultAdDisclosureMode) {
        this.defaultAdDisclosureMode = defaultAdDisclosureMode;
    }

    public Integer getDefaultSceneCountTarget() {
        return defaultSceneCountTarget;
    }

    public void setDefaultSceneCountTarget(Integer defaultSceneCountTarget) {
        this.defaultSceneCountTarget = defaultSceneCountTarget;
    }

    public String getDefaultCharacterConsistencyMode() {
        return defaultCharacterConsistencyMode;
    }

    public void setDefaultCharacterConsistencyMode(String defaultCharacterConsistencyMode) {
        this.defaultCharacterConsistencyMode = defaultCharacterConsistencyMode;
    }

    public String getDefaultNiche() {
        return defaultNiche;
    }

    public void setDefaultNiche(String defaultNiche) {
        this.defaultNiche = defaultNiche;
    }

    public String getDefaultPlatform() {
        return defaultPlatform;
    }

    public void setDefaultPlatform(String defaultPlatform) {
        this.defaultPlatform = defaultPlatform;
    }

    public String getDefaultSubtitleStyle() {
        return defaultSubtitleStyle;
    }

    public void setDefaultSubtitleStyle(String defaultSubtitleStyle) {
        this.defaultSubtitleStyle = defaultSubtitleStyle;
    }

    public String getDefaultVisualMode() {
        return defaultVisualMode;
    }

    public void setDefaultVisualMode(String defaultVisualMode) {
        this.defaultVisualMode = defaultVisualMode;
    }

    public String getDefaultVoiceProvider() {
        return defaultVoiceProvider;
    }

    public void setDefaultVoiceProvider(String defaultVoiceProvider) {
        this.defaultVoiceProvider = defaultVoiceProvider;
    }

    public String getDefaultVoicePersona() {
        return defaultVoicePersona;
    }

    public void setDefaultVoicePersona(String defaultVoicePersona) {
        this.defaultVoicePersona = defaultVoicePersona;
    }

    public String getDefaultQualityPreset() {
        return defaultQualityPreset;
    }

    public void setDefaultQualityPreset(String defaultQualityPreset) {
        this.defaultQualityPreset = defaultQualityPreset;
    }

    public Integer getDefaultDurationSeconds() {
        return defaultDurationSeconds;
    }

    public void setDefaultDurationSeconds(Integer defaultDurationSeconds) {
        this.defaultDurationSeconds = defaultDurationSeconds;
    }

    public Integer getDefaultVariantCount() {
        return defaultVariantCount;
    }

    public void setDefaultVariantCount(Integer defaultVariantCount) {
        this.defaultVariantCount = defaultVariantCount;
    }
}
