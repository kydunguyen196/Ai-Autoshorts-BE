package com.autoshorts.ai.controller;

import com.autoshorts.ai.dto.VideoTemplateRequest;
import com.autoshorts.ai.dto.VideoTemplateResponse;
import com.autoshorts.ai.service.CurrentUserService;
import com.autoshorts.ai.service.VideoTemplateService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
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
@RequestMapping("/api/templates")
@Validated
public class VideoTemplateController {

    private final VideoTemplateService videoTemplateService;
    private final CurrentUserService currentUserService;

    public VideoTemplateController(VideoTemplateService videoTemplateService, CurrentUserService currentUserService) {
        this.videoTemplateService = videoTemplateService;
        this.currentUserService = currentUserService;
    }

    @GetMapping
    public ResponseEntity<List<VideoTemplateResponse>> list() {
        UUID userId = currentUserService.requireCurrentUserId();
        return ResponseEntity.ok(videoTemplateService.list(userId));
    }

    @PostMapping
    public ResponseEntity<VideoTemplateResponse> create(@Valid @RequestBody VideoTemplateRequest request) {
        UUID userId = currentUserService.requireCurrentUserId();
        return ResponseEntity.status(HttpStatus.CREATED).body(videoTemplateService.create(userId, request));
    }

    @PutMapping("/{templateId}")
    public ResponseEntity<VideoTemplateResponse> update(
        @PathVariable UUID templateId,
        @Valid @RequestBody VideoTemplateRequest request
    ) {
        UUID userId = currentUserService.requireCurrentUserId();
        return ResponseEntity.ok(videoTemplateService.update(userId, templateId, request));
    }

    @DeleteMapping("/{templateId}")
    public ResponseEntity<Void> delete(@PathVariable UUID templateId) {
        UUID userId = currentUserService.requireCurrentUserId();
        videoTemplateService.delete(userId, templateId);
        return ResponseEntity.noContent().build();
    }
}
