package com.autoshorts.ai.dto;

import com.autoshorts.ai.entity.CharacterProfileStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public class CharacterProfileUpsertRequest {

    private UUID channelId;

    @NotBlank(message = "name is required")
    @Size(max = 120, message = "name must be <= 120 characters")
    private String name;

    @Size(max = 120, message = "archetype must be <= 120 characters")
    private String archetype;

    @Size(max = 4000, message = "personality must be <= 4000 characters")
    private String personality;

    @Size(max = 160, message = "toneOfVoice must be <= 160 characters")
    private String toneOfVoice;

    @Size(max = 160, message = "speakingStyle must be <= 160 characters")
    private String speakingStyle;

    @Size(max = 2000, message = "catchphrases must be <= 2000 characters")
    private String catchphrases;

    @Size(max = 4000, message = "visualStyle must be <= 4000 characters")
    private String visualStyle;

    @Size(max = 32, message = "language must be <= 32 characters")
    private String language;

    @Size(max = 255, message = "targetAudience must be <= 255 characters")
    private String targetAudience;

    @Size(max = 2000, message = "allowedTopics must be <= 2000 characters")
    private String allowedTopics;

    @Size(max = 2000, message = "forbiddenTopics must be <= 2000 characters")
    private String forbiddenTopics;

    @Size(max = 80, message = "defaultVoiceProvider must be <= 80 characters")
    private String defaultVoiceProvider;

    @Size(max = 200, message = "defaultVoiceId must be <= 200 characters")
    private String defaultVoiceId;

    private CharacterProfileStatus status = CharacterProfileStatus.ACTIVE;

    public UUID getChannelId() {
        return channelId;
    }

    public void setChannelId(UUID channelId) {
        this.channelId = channelId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getArchetype() {
        return archetype;
    }

    public void setArchetype(String archetype) {
        this.archetype = archetype;
    }

    public String getPersonality() {
        return personality;
    }

    public void setPersonality(String personality) {
        this.personality = personality;
    }

    public String getToneOfVoice() {
        return toneOfVoice;
    }

    public void setToneOfVoice(String toneOfVoice) {
        this.toneOfVoice = toneOfVoice;
    }

    public String getSpeakingStyle() {
        return speakingStyle;
    }

    public void setSpeakingStyle(String speakingStyle) {
        this.speakingStyle = speakingStyle;
    }

    public String getCatchphrases() {
        return catchphrases;
    }

    public void setCatchphrases(String catchphrases) {
        this.catchphrases = catchphrases;
    }

    public String getVisualStyle() {
        return visualStyle;
    }

    public void setVisualStyle(String visualStyle) {
        this.visualStyle = visualStyle;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public String getTargetAudience() {
        return targetAudience;
    }

    public void setTargetAudience(String targetAudience) {
        this.targetAudience = targetAudience;
    }

    public String getAllowedTopics() {
        return allowedTopics;
    }

    public void setAllowedTopics(String allowedTopics) {
        this.allowedTopics = allowedTopics;
    }

    public String getForbiddenTopics() {
        return forbiddenTopics;
    }

    public void setForbiddenTopics(String forbiddenTopics) {
        this.forbiddenTopics = forbiddenTopics;
    }

    public String getDefaultVoiceProvider() {
        return defaultVoiceProvider;
    }

    public void setDefaultVoiceProvider(String defaultVoiceProvider) {
        this.defaultVoiceProvider = defaultVoiceProvider;
    }

    public String getDefaultVoiceId() {
        return defaultVoiceId;
    }

    public void setDefaultVoiceId(String defaultVoiceId) {
        this.defaultVoiceId = defaultVoiceId;
    }

    public CharacterProfileStatus getStatus() {
        return status;
    }

    public void setStatus(CharacterProfileStatus status) {
        this.status = status;
    }
}
