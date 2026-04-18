package com.autoshorts.ai.controller;

import com.autoshorts.ai.dto.CharacterCampaignResponse;
import com.autoshorts.ai.dto.CharacterCampaignUpsertRequest;
import com.autoshorts.ai.service.CharacterCampaignService;
import com.autoshorts.ai.service.CurrentUserService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/characters/campaigns")
@Validated
public class CharacterCampaignController {

    private final CharacterCampaignService characterCampaignService;
    private final CurrentUserService currentUserService;

    public CharacterCampaignController(CharacterCampaignService characterCampaignService, CurrentUserService currentUserService) {
        this.characterCampaignService = characterCampaignService;
        this.currentUserService = currentUserService;
    }

    @GetMapping
    public ResponseEntity<List<CharacterCampaignResponse>> listCampaigns(@RequestParam(required = false) UUID channelId) {
        UUID userId = currentUserService.requireCurrentUserId();
        return ResponseEntity.ok(characterCampaignService.listCampaigns(userId, channelId));
    }

    @GetMapping("/{campaignId}")
    public ResponseEntity<CharacterCampaignResponse> getCampaign(@PathVariable UUID campaignId) {
        UUID userId = currentUserService.requireCurrentUserId();
        return ResponseEntity.ok(characterCampaignService.getCampaign(campaignId, userId));
    }

    @PostMapping
    public ResponseEntity<CharacterCampaignResponse> createCampaign(@Valid @RequestBody CharacterCampaignUpsertRequest request) {
        UUID userId = currentUserService.requireCurrentUserId();
        return ResponseEntity.ok(characterCampaignService.createCampaign(userId, request));
    }

    @PutMapping("/{campaignId}")
    public ResponseEntity<CharacterCampaignResponse> updateCampaign(
        @PathVariable UUID campaignId,
        @Valid @RequestBody CharacterCampaignUpsertRequest request
    ) {
        UUID userId = currentUserService.requireCurrentUserId();
        return ResponseEntity.ok(characterCampaignService.updateCampaign(campaignId, userId, request));
    }

    @DeleteMapping("/{campaignId}")
    public ResponseEntity<Void> deleteCampaign(@PathVariable UUID campaignId) {
        UUID userId = currentUserService.requireCurrentUserId();
        characterCampaignService.deleteCampaign(campaignId, userId);
        return ResponseEntity.noContent().build();
    }
}
