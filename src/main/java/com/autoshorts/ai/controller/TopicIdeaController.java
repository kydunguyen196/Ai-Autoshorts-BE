package com.autoshorts.ai.controller;

import com.autoshorts.ai.dto.TopicIdeaCreateRequest;
import com.autoshorts.ai.dto.TopicIdeaImportRequest;
import com.autoshorts.ai.dto.TopicIdeaImportResponse;
import com.autoshorts.ai.dto.TopicIdeaResponse;
import com.autoshorts.ai.dto.PagedResponse;
import com.autoshorts.ai.entity.TopicIdeaStatus;
import com.autoshorts.ai.service.CurrentUserService;
import com.autoshorts.ai.service.TopicIdeaService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/topics")
@Validated
public class TopicIdeaController {

    private final TopicIdeaService topicIdeaService;
    private final CurrentUserService currentUserService;

    public TopicIdeaController(TopicIdeaService topicIdeaService, CurrentUserService currentUserService) {
        this.topicIdeaService = topicIdeaService;
        this.currentUserService = currentUserService;
    }

    @GetMapping
    public ResponseEntity<List<TopicIdeaResponse>> listTopics(
        @RequestParam(defaultValue = "20") @Min(1) @Max(200) int limit,
        @RequestParam(required = false) TopicIdeaStatus status
    ) {
        return ResponseEntity.ok(topicIdeaService.listTopics(currentUserService.requireCurrentUserId(), limit, status));
    }

    @GetMapping("/feed")
    public ResponseEntity<PagedResponse<TopicIdeaResponse>> listTopicFeed(
        @RequestParam(defaultValue = "0") @Min(0) int page,
        @RequestParam(defaultValue = "20") @Min(1) @Max(200) int limit,
        @RequestParam(required = false) TopicIdeaStatus status
    ) {
        return ResponseEntity.ok(topicIdeaService.listTopicsPage(currentUserService.requireCurrentUserId(), page, limit, status));
    }

    @PostMapping
    public ResponseEntity<TopicIdeaResponse> createTopic(@Valid @RequestBody TopicIdeaCreateRequest request) {
        return ResponseEntity.ok(topicIdeaService.createTopic(currentUserService.requireCurrentUserId(), request));
    }

    @PostMapping("/import")
    public ResponseEntity<TopicIdeaImportResponse> importTopics(@Valid @RequestBody TopicIdeaImportRequest request) {
        return ResponseEntity.ok(topicIdeaService.importTopics(currentUserService.requireCurrentUserId(), request));
    }
}
