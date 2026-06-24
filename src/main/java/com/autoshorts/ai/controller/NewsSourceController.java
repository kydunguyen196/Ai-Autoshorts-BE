package com.autoshorts.ai.controller;

import com.autoshorts.ai.dto.NewsItemResponse;
import com.autoshorts.ai.dto.NewsSourceRequest;
import com.autoshorts.ai.dto.NewsSourceResponse;
import com.autoshorts.ai.dto.PagedResponse;
import com.autoshorts.ai.service.CurrentUserService;
import com.autoshorts.ai.service.NewsSourceService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
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
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/news")
@Validated
public class NewsSourceController {

    private final NewsSourceService newsSourceService;
    private final CurrentUserService currentUserService;

    public NewsSourceController(NewsSourceService newsSourceService, CurrentUserService currentUserService) {
        this.newsSourceService = newsSourceService;
        this.currentUserService = currentUserService;
    }

    @GetMapping("/sources")
    public ResponseEntity<List<NewsSourceResponse>> listSources() {
        return ResponseEntity.ok(newsSourceService.list(currentUserService.requireCurrentUserId()));
    }

    @PostMapping("/sources")
    public ResponseEntity<NewsSourceResponse> createSource(@Valid @RequestBody NewsSourceRequest request) {
        return ResponseEntity.ok(newsSourceService.create(currentUserService.requireCurrentUserId(), request));
    }

    @PutMapping("/sources/{id}")
    public ResponseEntity<NewsSourceResponse> updateSource(
        @PathVariable UUID id,
        @Valid @RequestBody NewsSourceRequest request
    ) {
        return ResponseEntity.ok(newsSourceService.update(currentUserService.requireCurrentUserId(), id, request));
    }

    @DeleteMapping("/sources/{id}")
    public ResponseEntity<Void> deleteSource(@PathVariable UUID id) {
        newsSourceService.delete(currentUserService.requireCurrentUserId(), id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/sources/{id}/fetch-now")
    public ResponseEntity<Map<String, Integer>> fetchNow(@PathVariable UUID id) {
        int created = newsSourceService.fetchNow(currentUserService.requireCurrentUserId(), id);
        return ResponseEntity.ok(Map.of("newItems", created));
    }

    @GetMapping("/items")
    public ResponseEntity<PagedResponse<NewsItemResponse>> listItems(
        @RequestParam(required = false) UUID sourceId,
        @RequestParam(defaultValue = "0") @Min(0) int page,
        @RequestParam(defaultValue = "20") @Min(1) @Max(100) int limit
    ) {
        return ResponseEntity.ok(
            newsSourceService.listItems(currentUserService.requireCurrentUserId(), sourceId, page, limit));
    }
}
