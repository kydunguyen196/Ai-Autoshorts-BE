package com.autoshorts.ai.controller;

import com.autoshorts.ai.dto.AdminBroadcastRequest;
import com.autoshorts.ai.dto.AdminCreditAdjustRequest;
import com.autoshorts.ai.dto.AdminOverviewResponse;
import com.autoshorts.ai.dto.AdminUserResponse;
import com.autoshorts.ai.dto.AdminUserUpdateRequest;
import com.autoshorts.ai.dto.AppSettingResponse;
import com.autoshorts.ai.dto.AppSettingUpdateRequest;
import com.autoshorts.ai.dto.NewsItemResponse;
import com.autoshorts.ai.dto.NewsSourceResponse;
import com.autoshorts.ai.dto.PagedResponse;
import com.autoshorts.ai.dto.VideoJobResponse;
import com.autoshorts.ai.entity.AppUser;
import com.autoshorts.ai.entity.JobStatus;
import com.autoshorts.ai.service.AdminService;
import com.autoshorts.ai.service.AuditService;
import com.autoshorts.ai.service.CurrentUserService;
import com.autoshorts.ai.service.NotificationService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
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
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
@Validated
public class AdminController {

    private final AdminService adminService;
    private final NotificationService notificationService;
    private final AuditService auditService;
    private final CurrentUserService currentUserService;

    public AdminController(
        AdminService adminService,
        NotificationService notificationService,
        AuditService auditService,
        CurrentUserService currentUserService
    ) {
        this.adminService = adminService;
        this.notificationService = notificationService;
        this.auditService = auditService;
        this.currentUserService = currentUserService;
    }

    @GetMapping("/overview")
    public ResponseEntity<AdminOverviewResponse> overview() {
        return ResponseEntity.ok(adminService.overview());
    }

    // --- Users ---
    @GetMapping("/users")
    public ResponseEntity<PagedResponse<AdminUserResponse>> listUsers(
        @RequestParam(required = false) String search,
        @RequestParam(defaultValue = "0") @Min(0) int page,
        @RequestParam(defaultValue = "20") @Min(1) @Max(100) int limit
    ) {
        return ResponseEntity.ok(adminService.listUsers(search, page, limit));
    }

    @PutMapping("/users/{id}")
    public ResponseEntity<AdminUserResponse> updateUser(
        @PathVariable UUID id,
        @RequestBody AdminUserUpdateRequest request
    ) {
        AdminUserResponse response = adminService.updateUser(id, request);
        audit("UPDATE_USER", "user", id.toString(),
            "role=" + request.getRole() + " enabled=" + request.getEnabled());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/users/{id}/credits")
    public ResponseEntity<Void> adjustCredits(
        @PathVariable UUID id,
        @Valid @RequestBody AdminCreditAdjustRequest request
    ) {
        adminService.adjustCredits(id, request.getAmount(), request.getReason());
        audit("ADJUST_CREDITS", "user", id.toString(),
            "amount=" + request.getAmount() + " reason=" + request.getReason());
        return ResponseEntity.noContent().build();
    }

    // --- Jobs ---
    @GetMapping("/jobs")
    public ResponseEntity<PagedResponse<VideoJobResponse>> listJobs(
        @RequestParam(required = false) JobStatus status,
        @RequestParam(defaultValue = "0") @Min(0) int page,
        @RequestParam(defaultValue = "20") @Min(1) @Max(100) int limit
    ) {
        return ResponseEntity.ok(adminService.listJobs(status, page, limit));
    }

    @PostMapping("/jobs/{id}/retry")
    public ResponseEntity<VideoJobResponse> retryJob(@PathVariable UUID id) {
        VideoJobResponse response = adminService.retryJob(id);
        audit("RETRY_JOB", "job", id.toString(), null);
        return ResponseEntity.accepted().body(response);
    }

    // --- Settings ---
    @GetMapping("/settings")
    public ResponseEntity<List<AppSettingResponse>> listSettings() {
        return ResponseEntity.ok(adminService.listSettings());
    }

    @PutMapping("/settings")
    public ResponseEntity<AppSettingResponse> updateSetting(@Valid @RequestBody AppSettingUpdateRequest request) {
        UUID adminId = currentUserService.requireCurrentUserId();
        AppSettingResponse response = adminService.updateSetting(
            request.getKey(), request.getValue(), request.getValueType(), request.getCategory(), adminId);
        audit("UPDATE_SETTING", "setting", request.getKey(), "value=" + request.getValue());
        return ResponseEntity.ok(response);
    }

    // --- News (global) ---
    @GetMapping("/news/sources")
    public ResponseEntity<PagedResponse<NewsSourceResponse>> listNewsSources(
        @RequestParam(defaultValue = "0") @Min(0) int page,
        @RequestParam(defaultValue = "20") @Min(1) @Max(100) int limit
    ) {
        return ResponseEntity.ok(adminService.listAllNewsSources(page, limit));
    }

    @GetMapping("/news/items")
    public ResponseEntity<PagedResponse<NewsItemResponse>> listNewsItems(
        @RequestParam(defaultValue = "0") @Min(0) int page,
        @RequestParam(defaultValue = "20") @Min(1) @Max(100) int limit
    ) {
        return ResponseEntity.ok(adminService.listAllNewsItems(page, limit));
    }

    // --- Notifications broadcast ---
    @PostMapping("/notifications/broadcast")
    public ResponseEntity<Map<String, Integer>> broadcast(@Valid @RequestBody AdminBroadcastRequest request) {
        int count = notificationService.broadcast(request.getTitle(), request.getMessage(), null);
        audit("BROADCAST", "notification", null, "recipients=" + count);
        return ResponseEntity.ok(Map.of("recipients", count));
    }

    private void audit(String action, String targetType, String targetId, String details) {
        AppUser actor = currentUserService.requireCurrentUserEntity();
        auditService.record(actor.getId(), actor.getEmail(), action, targetType, targetId, details);
    }
}
