package com.autoshorts.ai.controller;

import com.autoshorts.ai.dto.TikTokConnectionStatusResponse;
import com.autoshorts.ai.dto.TikTokConnectionUpsertRequest;
import com.autoshorts.ai.service.CurrentUserService;
import com.autoshorts.ai.service.TikTokConnectionService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/integrations/tiktok")
@Validated
public class TikTokIntegrationController {

    private final TikTokConnectionService tikTokConnectionService;
    private final CurrentUserService currentUserService;

    public TikTokIntegrationController(TikTokConnectionService tikTokConnectionService, CurrentUserService currentUserService) {
        this.tikTokConnectionService = tikTokConnectionService;
        this.currentUserService = currentUserService;
    }

    @PostMapping("/connection")
    public ResponseEntity<TikTokConnectionStatusResponse> upsertConnection(
        @Valid @RequestBody TikTokConnectionUpsertRequest request
    ) {
        UUID userId = currentUserService.requireCurrentUserId();
        return ResponseEntity.ok(tikTokConnectionService.upsertConnection(userId, request));
    }

    @GetMapping("/connection")
    public ResponseEntity<TikTokConnectionStatusResponse> getConnectionStatus(
        @RequestParam(required = false) UUID channelId
    ) {
        UUID userId = currentUserService.requireCurrentUserId();
        return ResponseEntity.ok(tikTokConnectionService.getConnectionStatus(userId, channelId));
    }
}
