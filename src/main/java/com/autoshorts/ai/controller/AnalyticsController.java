package com.autoshorts.ai.controller;

import com.autoshorts.ai.dto.analytics.AnalyticsSummaryResponse;
import com.autoshorts.ai.service.AnalyticsService;
import com.autoshorts.ai.service.CurrentUserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/analytics")
public class AnalyticsController {

    private final AnalyticsService analyticsService;
    private final CurrentUserService currentUserService;

    public AnalyticsController(AnalyticsService analyticsService, CurrentUserService currentUserService) {
        this.analyticsService = analyticsService;
        this.currentUserService = currentUserService;
    }

    @GetMapping("/summary")
    public ResponseEntity<AnalyticsSummaryResponse> getSummary(
        @RequestParam(name = "windowDays", defaultValue = "30") int windowDays
    ) {
        UUID userId = currentUserService.requireCurrentUserId();
        return ResponseEntity.ok(analyticsService.getSummary(userId, windowDays));
    }
}
