package com.autoshorts.ai.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "character_assets")
public class CharacterAsset {

    @Id
    private UUID id;

    @Column(name = "owner_user_id", nullable = false)
    private UUID ownerUserId;

    @Column(name = "channel_id", nullable = false)
    private UUID channelId;

    @Column(name = "character_profile_id", nullable = false)
    private UUID characterProfileId;

    @Column(name = "asset_type", length = 50)
    private String assetType;

    @Column(name = "reference_prompt", columnDefinition = "TEXT")
    private String referencePrompt;

    @Column(name = "style_metadata_json", columnDefinition = "TEXT")
    private String styleMetadataJson;

    @Column(length = 80)
    private String source;

    @Column(name = "storage_url", columnDefinition = "TEXT")
    private String storageUrl;

    @Column(nullable = false)
    private Boolean active = true;

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
        if (active == null) {
            active = true;
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

    public UUID getCharacterProfileId() {
        return characterProfileId;
    }

    public void setCharacterProfileId(UUID characterProfileId) {
        this.characterProfileId = characterProfileId;
    }

    public String getAssetType() {
        return assetType;
    }

    public void setAssetType(String assetType) {
        this.assetType = assetType;
    }

    public String getReferencePrompt() {
        return referencePrompt;
    }

    public void setReferencePrompt(String referencePrompt) {
        this.referencePrompt = referencePrompt;
    }

    public String getStyleMetadataJson() {
        return styleMetadataJson;
    }

    public void setStyleMetadataJson(String styleMetadataJson) {
        this.styleMetadataJson = styleMetadataJson;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getStorageUrl() {
        return storageUrl;
    }

    public void setStorageUrl(String storageUrl) {
        this.storageUrl = storageUrl;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
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
