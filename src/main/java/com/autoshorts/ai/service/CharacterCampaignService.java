package com.autoshorts.ai.service;

import com.autoshorts.ai.dto.CharacterCampaignResponse;
import com.autoshorts.ai.dto.CharacterCampaignUpsertRequest;
import com.autoshorts.ai.entity.CharacterCampaign;
import com.autoshorts.ai.entity.CharacterCampaignStatus;
import com.autoshorts.ai.entity.CharacterProfile;
import com.autoshorts.ai.exception.BadRequestException;
import com.autoshorts.ai.exception.ResourceNotFoundException;
import com.autoshorts.ai.repository.CharacterCampaignRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
public class CharacterCampaignService {

    private final CharacterCampaignRepository characterCampaignRepository;
    private final CharacterProfileService characterProfileService;
    private final ChannelService channelService;

    public CharacterCampaignService(
        CharacterCampaignRepository characterCampaignRepository,
        CharacterProfileService characterProfileService,
        ChannelService channelService
    ) {
        this.characterCampaignRepository = characterCampaignRepository;
        this.characterProfileService = characterProfileService;
        this.channelService = channelService;
    }

    @Transactional(readOnly = true)
    public List<CharacterCampaignResponse> listCampaigns(UUID userId, UUID channelId) {
        List<CharacterCampaign> campaigns = channelId == null
            ? characterCampaignRepository.findAllByOwnerUserIdOrderByCreatedAtDesc(userId)
            : characterCampaignRepository.findAllByOwnerUserIdAndChannelIdOrderByCreatedAtDesc(userId, channelId);
        return campaigns.stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public CharacterCampaignResponse getCampaign(UUID campaignId, UUID userId) {
        return toResponse(getEntityForUserOrThrow(campaignId, userId));
    }

    @Transactional
    public CharacterCampaignResponse createCampaign(UUID userId, CharacterCampaignUpsertRequest request) {
        UUID channelId = channelService.resolveOwnedChannelIdOrDefault(userId, request.getChannelId());
        CharacterCampaign campaign = new CharacterCampaign();
        campaign.setOwnerUserId(userId);
        campaign.setChannelId(channelId);
        applyFields(campaign, request, userId, channelId);
        return toResponse(characterCampaignRepository.save(campaign));
    }

    @Transactional
    public CharacterCampaignResponse updateCampaign(UUID campaignId, UUID userId, CharacterCampaignUpsertRequest request) {
        CharacterCampaign campaign = getEntityForUserOrThrow(campaignId, userId);
        UUID resolvedChannelId = request.getChannelId() == null
            ? campaign.getChannelId()
            : channelService.resolveOwnedChannelIdOrDefault(userId, request.getChannelId());
        campaign.setChannelId(resolvedChannelId);
        applyFields(campaign, request, userId, resolvedChannelId);
        return toResponse(characterCampaignRepository.save(campaign));
    }

    @Transactional
    public void deleteCampaign(UUID campaignId, UUID userId) {
        CharacterCampaign campaign = getEntityForUserOrThrow(campaignId, userId);
        characterCampaignRepository.delete(campaign);
    }

    @Transactional(readOnly = true)
    public CharacterCampaign getEntityForUserOrThrow(UUID campaignId, UUID userId) {
        return characterCampaignRepository.findByIdAndOwnerUserId(campaignId, userId)
            .orElseThrow(() -> new ResourceNotFoundException("Character campaign not found: " + campaignId));
    }

    private void applyFields(CharacterCampaign campaign, CharacterCampaignUpsertRequest request, UUID userId, UUID channelId) {
        campaign.setProductName(request.getProductName().trim());
        campaign.setProductType(trimToNull(request.getProductType()));
        campaign.setProductDescription(trimToNull(request.getProductDescription()));
        campaign.setProductUrl(trimToNull(request.getProductUrl()));
        campaign.setTargetPlatform(trimToNull(request.getTargetPlatform()));
        campaign.setCampaignObjective(trimToNull(request.getCampaignObjective()));
        campaign.setCallToAction(trimToNull(request.getCallToAction()));
        campaign.setTargetAudience(trimToNull(request.getTargetAudience()));
        campaign.setOfferSummary(trimToNull(request.getOfferSummary()));
        campaign.setStatus(request.getStatus() == null ? CharacterCampaignStatus.DRAFT : request.getStatus());

        UUID characterProfileId = request.getCharacterProfileId();
        if (characterProfileId == null) {
            campaign.setCharacterProfileId(null);
            return;
        }

        CharacterProfile profile = characterProfileService.getEntityForUserOrThrow(characterProfileId, userId);
        if (!Objects.equals(profile.getChannelId(), channelId)) {
            throw new BadRequestException("characterProfileId must belong to the same channel as campaign");
        }
        campaign.setCharacterProfileId(profile.getId());
    }

    private CharacterCampaignResponse toResponse(CharacterCampaign campaign) {
        CharacterCampaignResponse response = new CharacterCampaignResponse();
        response.setId(campaign.getId());
        response.setChannelId(campaign.getChannelId());
        response.setCharacterProfileId(campaign.getCharacterProfileId());
        response.setProductName(campaign.getProductName());
        response.setProductType(campaign.getProductType());
        response.setProductDescription(campaign.getProductDescription());
        response.setProductUrl(campaign.getProductUrl());
        response.setTargetPlatform(campaign.getTargetPlatform());
        response.setCampaignObjective(campaign.getCampaignObjective());
        response.setCallToAction(campaign.getCallToAction());
        response.setTargetAudience(campaign.getTargetAudience());
        response.setOfferSummary(campaign.getOfferSummary());
        response.setStatus(campaign.getStatus());
        response.setCreatedAt(campaign.getCreatedAt());
        response.setUpdatedAt(campaign.getUpdatedAt());
        return response;
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }
}
