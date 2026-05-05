package com.autoshorts.ai.controller;

import com.autoshorts.ai.dto.BillingCheckoutRequest;
import com.autoshorts.ai.dto.BillingCreditsResponse;
import com.autoshorts.ai.dto.BillingPlanResponse;
import com.autoshorts.ai.dto.BillingPortalResponse;
import com.autoshorts.ai.dto.BillingSubscriptionResponse;
import com.autoshorts.ai.dto.BillingUsageEntryResponse;
import com.autoshorts.ai.service.BillingService;
import com.autoshorts.ai.service.CurrentUserService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/billing")
public class BillingController {

    private final BillingService billingService;
    private final CurrentUserService currentUserService;

    public BillingController(BillingService billingService, CurrentUserService currentUserService) {
        this.billingService = billingService;
        this.currentUserService = currentUserService;
    }

    @GetMapping("/plans")
    public ResponseEntity<List<BillingPlanResponse>> listPlans() {
        return ResponseEntity.ok(billingService.listPlans());
    }

    @GetMapping("/subscription")
    public ResponseEntity<BillingSubscriptionResponse> getSubscription() {
        UUID userId = currentUserService.requireCurrentUserId();
        return ResponseEntity.ok(billingService.getSubscription(userId));
    }

    @GetMapping("/credits")
    public ResponseEntity<BillingCreditsResponse> getCredits() {
        UUID userId = currentUserService.requireCurrentUserId();
        return ResponseEntity.ok(billingService.getCredits(userId));
    }

    @GetMapping("/usage")
    public ResponseEntity<List<BillingUsageEntryResponse>> getUsage() {
        UUID userId = currentUserService.requireCurrentUserId();
        return ResponseEntity.ok(billingService.listUsage(userId));
    }

    @PostMapping("/checkout")
    public ResponseEntity<BillingPortalResponse> createCheckout(@Valid @RequestBody BillingCheckoutRequest request) {
        UUID userId = currentUserService.requireCurrentUserId();
        return ResponseEntity.ok(billingService.createCheckout(userId, request.getPlanKey()));
    }

    @PostMapping("/portal")
    public ResponseEntity<BillingPortalResponse> createPortal() {
        UUID userId = currentUserService.requireCurrentUserId();
        return ResponseEntity.ok(billingService.createPortal(userId));
    }

    @PostMapping("/webhook")
    public ResponseEntity<Void> handleStripeWebhook(
        @RequestBody String payload,
        @RequestHeader(name = "Stripe-Signature", required = false) String signature
    ) {
        billingService.handleStripeWebhook(payload, signature);
        return ResponseEntity.ok().build();
    }
}
