package com.autoshorts.ai.dto;

import com.autoshorts.ai.entity.CharacterCampaignStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public class CharacterCampaignUpsertRequest {

    private UUID channelId;

    private UUID characterProfileId;

    @NotBlank(message = "productName is required")
    @Size(max = 180, message = "productName must be <= 180 characters")
    private String productName;

    @Size(max = 120, message = "productType must be <= 120 characters")
    private String productType;

    @Size(max = 4000, message = "productDescription must be <= 4000 characters")
    private String productDescription;

    @Size(max = 2000, message = "productUrl must be <= 2000 characters")
    private String productUrl;

    @Size(max = 80, message = "targetPlatform must be <= 80 characters")
    private String targetPlatform;

    @Size(max = 200, message = "campaignObjective must be <= 200 characters")
    private String campaignObjective;

    @Size(max = 2000, message = "callToAction must be <= 2000 characters")
    private String callToAction;

    @Size(max = 255, message = "targetAudience must be <= 255 characters")
    private String targetAudience;

    @Size(max = 4000, message = "offerSummary must be <= 4000 characters")
    private String offerSummary;

    private CharacterCampaignStatus status = CharacterCampaignStatus.DRAFT;

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

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public String getProductType() {
        return productType;
    }

    public void setProductType(String productType) {
        this.productType = productType;
    }

    public String getProductDescription() {
        return productDescription;
    }

    public void setProductDescription(String productDescription) {
        this.productDescription = productDescription;
    }

    public String getProductUrl() {
        return productUrl;
    }

    public void setProductUrl(String productUrl) {
        this.productUrl = productUrl;
    }

    public String getTargetPlatform() {
        return targetPlatform;
    }

    public void setTargetPlatform(String targetPlatform) {
        this.targetPlatform = targetPlatform;
    }

    public String getCampaignObjective() {
        return campaignObjective;
    }

    public void setCampaignObjective(String campaignObjective) {
        this.campaignObjective = campaignObjective;
    }

    public String getCallToAction() {
        return callToAction;
    }

    public void setCallToAction(String callToAction) {
        this.callToAction = callToAction;
    }

    public String getTargetAudience() {
        return targetAudience;
    }

    public void setTargetAudience(String targetAudience) {
        this.targetAudience = targetAudience;
    }

    public String getOfferSummary() {
        return offerSummary;
    }

    public void setOfferSummary(String offerSummary) {
        this.offerSummary = offerSummary;
    }

    public CharacterCampaignStatus getStatus() {
        return status;
    }

    public void setStatus(CharacterCampaignStatus status) {
        this.status = status;
    }
}
