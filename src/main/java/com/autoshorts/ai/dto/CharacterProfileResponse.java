package com.autoshorts.ai.dto;

import com.autoshorts.ai.entity.CharacterProfileStatus;

import java.time.Instant;
import java.util.UUID;

public class CharacterProfileResponse {

    private UUID id;
    private UUID channelId;
    private String name;
    private String archetype;
    private String personality;
    private String toneOfVoice;
    private String speakingStyle;
    private String catchphrases;
    private String visualStyle;
    private String language;
    private String targetAudience;
    private String allowedTopics;
    private String forbiddenTopics;
    private String defaultVoiceProvider;
    private String defaultVoiceId;
    private CharacterProfileStatus status;
    private Instant createdAt;
    private Instant updatedAt;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

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
