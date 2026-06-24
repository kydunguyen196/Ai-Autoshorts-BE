package com.autoshorts.ai.controller;

import com.autoshorts.ai.dto.TikTokAuthorizationResponse;
import com.autoshorts.ai.dto.TikTokConnectionStatusResponse;
import com.autoshorts.ai.dto.TikTokConnectionUpsertRequest;
import com.autoshorts.ai.service.CurrentUserService;
import com.autoshorts.ai.service.TikTokConnectionService;
import com.autoshorts.ai.service.TikTokOAuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/api/integrations/tiktok")
@Validated
public class TikTokIntegrationController {

    private final TikTokConnectionService tikTokConnectionService;
    private final TikTokOAuthService tikTokOAuthService;
    private final CurrentUserService currentUserService;

    public TikTokIntegrationController(
        TikTokConnectionService tikTokConnectionService,
        TikTokOAuthService tikTokOAuthService,
        CurrentUserService currentUserService
    ) {
        this.tikTokConnectionService = tikTokConnectionService;
        this.tikTokOAuthService = tikTokOAuthService;
        this.currentUserService = currentUserService;
    }

    /**
     * Start the OAuth flow. Authenticated XHR from the dashboard; returns the TikTok authorize
     * URL the frontend should open/redirect to.
     */
    @GetMapping("/authorize")
    public ResponseEntity<TikTokAuthorizationResponse> authorize(@RequestParam(required = false) UUID channelId) {
        UUID userId = currentUserService.requireCurrentUserId();
        String url = tikTokOAuthService.buildAuthorizationUrl(userId, channelId);
        return ResponseEntity.ok(new TikTokAuthorizationResponse(url));
    }

    /**
     * OAuth redirect target. Public (no app JWT) — identity is carried in the signed {@code state}.
     * Exchanges the code for tokens, then 302-redirects the browser back to the frontend.
     */
    @GetMapping("/callback")
    public ResponseEntity<Void> callback(
        @RequestParam(required = false) String code,
        @RequestParam(required = false) String state,
        @RequestParam(required = false) String error
    ) {
        String redirectUrl = tikTokOAuthService.handleCallback(code, state, error);
        return ResponseEntity.status(HttpStatus.FOUND).location(URI.create(redirectUrl)).build();
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
