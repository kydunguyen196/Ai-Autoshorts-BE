package com.autoshorts.ai.controller;

import com.autoshorts.ai.dto.NotificationResponse;
import com.autoshorts.ai.dto.PagedResponse;
import com.autoshorts.ai.service.CurrentUserService;
import com.autoshorts.ai.service.NotificationService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/notifications")
@Validated
public class NotificationController {

    private final NotificationService notificationService;
    private final CurrentUserService currentUserService;

    public NotificationController(NotificationService notificationService, CurrentUserService currentUserService) {
        this.notificationService = notificationService;
        this.currentUserService = currentUserService;
    }

    @GetMapping
    public ResponseEntity<PagedResponse<NotificationResponse>> list(
        @RequestParam(defaultValue = "0") @Min(0) int page,
        @RequestParam(defaultValue = "20") @Min(1) @Max(100) int limit,
        @RequestParam(defaultValue = "false") boolean unread
    ) {
        UUID userId = currentUserService.requireCurrentUserId();
        return ResponseEntity.ok(PagedResponse.from(notificationService.list(userId, page, limit, unread)));
    }

    @GetMapping("/unread-count")
    public ResponseEntity<Map<String, Long>> unreadCount() {
        UUID userId = currentUserService.requireCurrentUserId();
        return ResponseEntity.ok(Map.of("count", notificationService.unreadCount(userId)));
    }

    @PostMapping("/{id}/read")
    public ResponseEntity<Void> markRead(@PathVariable UUID id) {
        notificationService.markRead(currentUserService.requireCurrentUserId(), id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/read-all")
    public ResponseEntity<Map<String, Integer>> markAllRead() {
        int updated = notificationService.markAllRead(currentUserService.requireCurrentUserId());
        return ResponseEntity.ok(Map.of("updated", updated));
    }
}
