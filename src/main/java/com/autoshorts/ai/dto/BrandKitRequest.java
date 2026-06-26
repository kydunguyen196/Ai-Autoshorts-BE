package com.autoshorts.ai.dto;

import jakarta.validation.constraints.Size;

/** Per-channel brand kit (logo overlay + colors + intro/outro clips). All fields optional. */
public class BrandKitRequest {

    @Size(max = 2000, message = "brandLogoUrl must be <= 2000 characters")
    private String brandLogoUrl;

    @Size(max = 16)
    private String brandPrimaryColor;

    @Size(max = 16)
    private String brandAccentColor;

    @Size(max = 2000, message = "brandIntroUrl must be <= 2000 characters")
    private String brandIntroUrl;

    @Size(max = 2000, message = "brandOutroUrl must be <= 2000 characters")
    private String brandOutroUrl;

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
}
