package com.autoshorts.ai.controller;

import com.autoshorts.ai.dto.CharacterProfileResponse;
import com.autoshorts.ai.dto.CharacterProfileUpsertRequest;
import com.autoshorts.ai.service.CharacterProfileService;
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
@RequestMapping("/api/characters/profiles")
@Validated
public class CharacterProfileController {

    private final CharacterProfileService characterProfileService;
    private final CurrentUserService currentUserService;

    public CharacterProfileController(CharacterProfileService characterProfileService, CurrentUserService currentUserService) {
        this.characterProfileService = characterProfileService;
        this.currentUserService = currentUserService;
    }

    @GetMapping
    public ResponseEntity<List<CharacterProfileResponse>> listProfiles(@RequestParam(required = false) UUID channelId) {
        UUID userId = currentUserService.requireCurrentUserId();
        return ResponseEntity.ok(characterProfileService.listProfiles(userId, channelId));
    }

    @GetMapping("/{profileId}")
    public ResponseEntity<CharacterProfileResponse> getProfile(@PathVariable UUID profileId) {
        UUID userId = currentUserService.requireCurrentUserId();
        return ResponseEntity.ok(characterProfileService.getProfile(profileId, userId));
    }

    @PostMapping
    public ResponseEntity<CharacterProfileResponse> createProfile(@Valid @RequestBody CharacterProfileUpsertRequest request) {
        UUID userId = currentUserService.requireCurrentUserId();
        return ResponseEntity.ok(characterProfileService.createProfile(userId, request));
    }

    @PutMapping("/{profileId}")
    public ResponseEntity<CharacterProfileResponse> updateProfile(
        @PathVariable UUID profileId,
        @Valid @RequestBody CharacterProfileUpsertRequest request
    ) {
        UUID userId = currentUserService.requireCurrentUserId();
        return ResponseEntity.ok(characterProfileService.updateProfile(profileId, userId, request));
    }

    @DeleteMapping("/{profileId}")
    public ResponseEntity<Void> deleteProfile(@PathVariable UUID profileId) {
        UUID userId = currentUserService.requireCurrentUserId();
        characterProfileService.deleteProfile(profileId, userId);
        return ResponseEntity.noContent().build();
    }
}
