package com.autoshorts.ai.controller;

import com.autoshorts.ai.dto.SocialConnectionStatusResponse;
import com.autoshorts.ai.dto.SocialConnectionUpsertRequest;
import com.autoshorts.ai.entity.SocialPlatform;
import com.autoshorts.ai.exception.ResourceNotFoundException;
import com.autoshorts.ai.service.CurrentUserService;
import com.autoshorts.ai.service.SocialConnectionService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Manage YouTube / Instagram account connections. One controller, platform in the path:
 * {@code /api/integrations/youtube/connection}, {@code /api/integrations/instagram/connection}.
 * TikTok keeps its own controller (it has a full OAuth redirect flow).
 */
@RestController
@RequestMapping("/api/integrations/{platform}")
@Validated
public class SocialIntegrationController {

    private final SocialConnectionService socialConnectionService;
    private final CurrentUserService currentUserService;

    public SocialIntegrationController(
        SocialConnectionService socialConnectionService,
        CurrentUserService currentUserService
    ) {
        this.socialConnectionService = socialConnectionService;
        this.currentUserService = currentUserService;
    }

    @PostMapping("/connection")
    public ResponseEntity<SocialConnectionStatusResponse> upsertConnection(
        @PathVariable String platform,
        @Valid @RequestBody SocialConnectionUpsertRequest request
    ) {
        UUID userId = currentUserService.requireCurrentUserId();
        return ResponseEntity.ok(socialConnectionService.upsertConnection(userId, resolvePlatform(platform), request));
    }

    @GetMapping("/connection")
    public ResponseEntity<SocialConnectionStatusResponse> getConnectionStatus(
        @PathVariable String platform,
        @RequestParam(required = false) UUID channelId
    ) {
        UUID userId = currentUserService.requireCurrentUserId();
        return ResponseEntity.ok(socialConnectionService.getConnectionStatus(userId, resolvePlatform(platform), channelId));
    }

    private SocialPlatform resolvePlatform(String platform) {
        return SocialPlatform.fromKey(platform)
            .orElseThrow(() -> new ResourceNotFoundException("Unsupported social platform: " + platform));
    }
}
