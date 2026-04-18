package com.autoshorts.ai.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "character_profiles")
public class CharacterProfile {

    @Id
    private UUID id;

    @Column(name = "owner_user_id", nullable = false)
    private UUID ownerUserId;

    @Column(name = "channel_id", nullable = false)
    private UUID channelId;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(length = 120)
    private String archetype;

    @Column(columnDefinition = "TEXT")
    private String personality;

    @Column(name = "tone_of_voice", length = 160)
    private String toneOfVoice;

    @Column(name = "speaking_style", length = 160)
    private String speakingStyle;

    @Column(columnDefinition = "TEXT")
    private String catchphrases;

    @Column(name = "visual_style", columnDefinition = "TEXT")
    private String visualStyle;

    @Column(length = 32)
    private String language;

    @Column(name = "target_audience", length = 255)
    private String targetAudience;

    @Column(name = "allowed_topics", columnDefinition = "TEXT")
    private String allowedTopics;

    @Column(name = "forbidden_topics", columnDefinition = "TEXT")
    private String forbiddenTopics;

    @Column(name = "default_voice_provider", length = 80)
    private String defaultVoiceProvider;

    @Column(name = "default_voice_id", length = 200)
    private String defaultVoiceId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private CharacterProfileStatus status = CharacterProfileStatus.ACTIVE;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    public void onCreate() {
        Instant now = Instant.now();
        if (id == null) {
            id = UUID.randomUUID();
        }
        if (status == null) {
            status = CharacterProfileStatus.ACTIVE;
        }
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

    public UUID getOwnerUserId() {
        return ownerUserId;
    }

    public void setOwnerUserId(UUID ownerUserId) {
        this.ownerUserId = ownerUserId;
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
