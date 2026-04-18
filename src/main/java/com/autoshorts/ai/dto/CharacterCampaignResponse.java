package com.autoshorts.ai.dto;

import com.autoshorts.ai.entity.CharacterCampaignStatus;

import java.time.Instant;
import java.util.UUID;

public class CharacterCampaignResponse {

    private UUID id;
    private UUID channelId;
    private UUID characterProfileId;
    private String productName;
    private String productType;
    private String productDescription;
    private String productUrl;
    private String targetPlatform;
    private String campaignObjective;
    private String callToAction;
    private String targetAudience;
    private String offerSummary;
    private CharacterCampaignStatus status;
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
