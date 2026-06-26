package com.autoshorts.ai.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class VideoTemplateRequest {

    @NotBlank(message = "name is required")
    @Size(max = 150, message = "name must be <= 150 characters")
    private String name;

    @Size(max = 500, message = "description must be <= 500 characters")
    private String description;

    @Size(max = 32)
    private String captionPosition;

    @Size(max = 120)
    private String fontFamily;

    @Size(max = 16)
    private String primaryColor;

    @Size(max = 16)
    private String accentColor;

    private boolean makeDefault;

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

    public String getCaptionPosition() {
        return captionPosition;
    }

    public void setCaptionPosition(String captionPosition) {
        this.captionPosition = captionPosition;
    }

    public String getFontFamily() {
        return fontFamily;
    }

    public void setFontFamily(String fontFamily) {
        this.fontFamily = fontFamily;
    }

    public String getPrimaryColor() {
        return primaryColor;
    }

    public void setPrimaryColor(String primaryColor) {
        this.primaryColor = primaryColor;
    }

    public String getAccentColor() {
        return accentColor;
    }

    public void setAccentColor(String accentColor) {
        this.accentColor = accentColor;
    }

    public boolean isMakeDefault() {
        return makeDefault;
    }

    public void setMakeDefault(boolean makeDefault) {
        this.makeDefault = makeDefault;
    }
}
