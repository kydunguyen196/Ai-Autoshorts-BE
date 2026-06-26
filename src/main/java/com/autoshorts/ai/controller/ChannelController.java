package com.autoshorts.ai.controller;

import com.autoshorts.ai.dto.BrandKitRequest;
import com.autoshorts.ai.dto.ChannelCreateRequest;
import com.autoshorts.ai.dto.ChannelResponse;
import com.autoshorts.ai.service.ChannelService;
import com.autoshorts.ai.service.CurrentUserService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/channels")
@Validated
public class ChannelController {

    private final ChannelService channelService;
    private final CurrentUserService currentUserService;

    public ChannelController(ChannelService channelService, CurrentUserService currentUserService) {
        this.channelService = channelService;
        this.currentUserService = currentUserService;
    }

    @GetMapping
    public ResponseEntity<List<ChannelResponse>> listMyChannels() {
        UUID userId = currentUserService.requireCurrentUserId();
        return ResponseEntity.ok(channelService.listChannels(userId));
    }

    @PostMapping
    public ResponseEntity<ChannelResponse> createChannel(@Valid @RequestBody ChannelCreateRequest request) {
        UUID userId = currentUserService.requireCurrentUserId();
        return ResponseEntity.ok(channelService.createChannel(userId, request));
    }

    @PutMapping("/{channelId}/brand-kit")
    public ResponseEntity<ChannelResponse> updateBrandKit(
        @PathVariable UUID channelId,
        @Valid @RequestBody BrandKitRequest request
    ) {
        UUID userId = currentUserService.requireCurrentUserId();
        return ResponseEntity.ok(channelService.updateBrandKit(userId, channelId, request));
    }
}
