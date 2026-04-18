package com.autoshorts.ai.service;

import com.autoshorts.ai.dto.CharacterProfileResponse;
import com.autoshorts.ai.dto.CharacterProfileUpsertRequest;
import com.autoshorts.ai.entity.CharacterProfile;
import com.autoshorts.ai.entity.CharacterProfileStatus;
import com.autoshorts.ai.exception.ResourceNotFoundException;
import com.autoshorts.ai.repository.CharacterProfileRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.UUID;

@Service
public class CharacterProfileService {

    private final CharacterProfileRepository characterProfileRepository;
    private final ChannelService channelService;

    public CharacterProfileService(
        CharacterProfileRepository characterProfileRepository,
        ChannelService channelService
    ) {
        this.characterProfileRepository = characterProfileRepository;
        this.channelService = channelService;
    }

    @Transactional(readOnly = true)
    public List<CharacterProfileResponse> listProfiles(UUID userId, UUID channelId) {
        List<CharacterProfile> profiles = channelId == null
            ? characterProfileRepository.findAllByOwnerUserIdOrderByCreatedAtDesc(userId)
            : characterProfileRepository.findAllByOwnerUserIdAndChannelIdOrderByCreatedAtDesc(userId, channelId);
        return profiles.stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public CharacterProfileResponse getProfile(UUID profileId, UUID userId) {
        CharacterProfile profile = getEntityForUserOrThrow(profileId, userId);
        return toResponse(profile);
    }

    @Transactional
    public CharacterProfileResponse createProfile(UUID userId, CharacterProfileUpsertRequest request) {
        UUID channelId = channelService.resolveOwnedChannelIdOrDefault(userId, request.getChannelId());
        CharacterProfile profile = new CharacterProfile();
        profile.setOwnerUserId(userId);
        profile.setChannelId(channelId);
        applyFields(profile, request);
        return toResponse(characterProfileRepository.save(profile));
    }

    @Transactional
    public CharacterProfileResponse updateProfile(UUID profileId, UUID userId, CharacterProfileUpsertRequest request) {
        CharacterProfile profile = getEntityForUserOrThrow(profileId, userId);
        UUID resolvedChannelId = request.getChannelId() == null
            ? profile.getChannelId()
            : channelService.resolveOwnedChannelIdOrDefault(userId, request.getChannelId());
        profile.setChannelId(resolvedChannelId);
        applyFields(profile, request);
        return toResponse(characterProfileRepository.save(profile));
    }

    @Transactional
    public void deleteProfile(UUID profileId, UUID userId) {
        CharacterProfile profile = getEntityForUserOrThrow(profileId, userId);
        characterProfileRepository.delete(profile);
    }

    @Transactional(readOnly = true)
    public CharacterProfile getEntityForUserOrThrow(UUID profileId, UUID userId) {
        return characterProfileRepository.findByIdAndOwnerUserId(profileId, userId)
            .orElseThrow(() -> new ResourceNotFoundException("Character profile not found: " + profileId));
    }

    private void applyFields(CharacterProfile profile, CharacterProfileUpsertRequest request) {
        profile.setName(request.getName().trim());
        profile.setArchetype(trimToNull(request.getArchetype()));
        profile.setPersonality(trimToNull(request.getPersonality()));
        profile.setToneOfVoice(trimToNull(request.getToneOfVoice()));
        profile.setSpeakingStyle(trimToNull(request.getSpeakingStyle()));
        profile.setCatchphrases(trimToNull(request.getCatchphrases()));
        profile.setVisualStyle(trimToNull(request.getVisualStyle()));
        profile.setLanguage(trimToNull(request.getLanguage()));
        profile.setTargetAudience(trimToNull(request.getTargetAudience()));
        profile.setAllowedTopics(trimToNull(request.getAllowedTopics()));
        profile.setForbiddenTopics(trimToNull(request.getForbiddenTopics()));
        profile.setDefaultVoiceProvider(trimToNull(request.getDefaultVoiceProvider()));
        profile.setDefaultVoiceId(trimToNull(request.getDefaultVoiceId()));
        profile.setStatus(request.getStatus() == null ? CharacterProfileStatus.ACTIVE : request.getStatus());
    }

    private CharacterProfileResponse toResponse(CharacterProfile profile) {
        CharacterProfileResponse response = new CharacterProfileResponse();
        response.setId(profile.getId());
        response.setChannelId(profile.getChannelId());
        response.setName(profile.getName());
        response.setArchetype(profile.getArchetype());
        response.setPersonality(profile.getPersonality());
        response.setToneOfVoice(profile.getToneOfVoice());
        response.setSpeakingStyle(profile.getSpeakingStyle());
        response.setCatchphrases(profile.getCatchphrases());
        response.setVisualStyle(profile.getVisualStyle());
        response.setLanguage(profile.getLanguage());
        response.setTargetAudience(profile.getTargetAudience());
        response.setAllowedTopics(profile.getAllowedTopics());
        response.setForbiddenTopics(profile.getForbiddenTopics());
        response.setDefaultVoiceProvider(profile.getDefaultVoiceProvider());
        response.setDefaultVoiceId(profile.getDefaultVoiceId());
        response.setStatus(profile.getStatus());
        response.setCreatedAt(profile.getCreatedAt());
        response.setUpdatedAt(profile.getUpdatedAt());
        return response;
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }
}
