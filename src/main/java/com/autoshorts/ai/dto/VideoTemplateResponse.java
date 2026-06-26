package com.autoshorts.ai.dto;

import com.autoshorts.ai.entity.VideoTemplate;

import java.time.Instant;
import java.util.UUID;

public class VideoTemplateResponse {

    private UUID id;
    private String name;
    private String description;
    private String captionPosition;
    private String fontFamily;
    private String primaryColor;
    private String accentColor;
    private boolean isDefault;
    private Instant createdAt;
    private Instant updatedAt;

    public static VideoTemplateResponse from(VideoTemplate template) {
        VideoTemplateResponse response = new VideoTemplateResponse();
        response.id = template.getId();
        response.name = template.getName();
        response.description = template.getDescription();
        response.captionPosition = template.getCaptionPosition();
        response.fontFamily = template.getFontFamily();
        response.primaryColor = template.getPrimaryColor();
        response.accentColor = template.getAccentColor();
        response.isDefault = template.isDefault();
        response.createdAt = template.getCreatedAt();
        response.updatedAt = template.getUpdatedAt();
        return response;
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getCaptionPosition() {
        return captionPosition;
    }

    public String getFontFamily() {
        return fontFamily;
    }

    public String getPrimaryColor() {
        return primaryColor;
    }

    public String getAccentColor() {
        return accentColor;
    }

    public boolean isDefault() {
        return isDefault;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
