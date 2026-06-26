package com.autoshorts.ai.dto;

import java.time.Instant;
import java.util.UUID;

public class ChannelResponse {

    private UUID id;
    private String name;
    private String description;
    private Boolean isDefault;
    private String brandLogoUrl;
    private String brandPrimaryColor;
    private String brandAccentColor;
    private String brandIntroUrl;
    private String brandOutroUrl;
    private Instant createdAt;
    private Instant updatedAt;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
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
