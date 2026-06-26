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
@Table(name = "channels")
public class Channel {

    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(length = 500)
    private String description;

    @Column(name = "is_default", nullable = false)
    private Boolean isDefault = false;

    @Column(name = "brand_logo_url", columnDefinition = "TEXT")
    private String brandLogoUrl;

    @Column(name = "brand_primary_color", length = 16)
    private String brandPrimaryColor;

    @Column(name = "brand_accent_color", length = 16)
    private String brandAccentColor;

    @Column(name = "brand_intro_url", columnDefinition = "TEXT")
    private String brandIntroUrl;

    @Column(name = "brand_outro_url", columnDefinition = "TEXT")
    private String brandOutroUrl;

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
        if (isDefault == null) {
            isDefault = false;
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

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Boolean getIsDefault() {
        return isDefault;
    }

    public void setIsDefault(Boolean isDefault) {
        this.isDefault = isDefault;
    }

    public String getBrandLogoUrl() {
        return brandLogoUrl;
    }

    public void setBrandLogoUrl(String brandLogoUrl) {
        this.brandLogoUrl = brandLogoUrl;
    }

    public String getBrandPrimaryColor() {
        return brandPrimaryColor;
    }

    public void setBrandPrimaryColor(String brandPrimaryColor) {
        this.brandPrimaryColor = brandPrimaryColor;
    }

    public String getBrandAccentColor() {
        return brandAccentColor;
    }

    public void setBrandAccentColor(String brandAccentColor) {
        this.brandAccentColor = brandAccentColor;
    }

    public String getBrandIntroUrl() {
        return brandIntroUrl;
    }

    public void setBrandIntroUrl(String brandIntroUrl) {
        this.brandIntroUrl = brandIntroUrl;
    }

    public String getBrandOutroUrl() {
        return brandOutroUrl;
    }

    public void setBrandOutroUrl(String brandOutroUrl) {
        this.brandOutroUrl = brandOutroUrl;
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
