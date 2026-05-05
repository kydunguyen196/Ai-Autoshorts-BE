package com.autoshorts.ai.service;

import com.autoshorts.ai.config.AppProperties;
import com.autoshorts.ai.dto.BillingCreditsResponse;
import com.autoshorts.ai.dto.BillingPlanResponse;
import com.autoshorts.ai.dto.BillingPortalResponse;
import com.autoshorts.ai.dto.BillingSubscriptionResponse;
import com.autoshorts.ai.dto.BillingUsageEntryResponse;
import com.autoshorts.ai.entity.AppUser;
import com.autoshorts.ai.entity.BillingSubscription;
import com.autoshorts.ai.entity.CreditLedgerEntry;
import com.autoshorts.ai.exception.BadRequestException;
import com.autoshorts.ai.exception.ExternalServiceException;
import com.autoshorts.ai.repository.AppUserRepository;
import com.autoshorts.ai.repository.BillingSubscriptionRepository;
import com.autoshorts.ai.repository.CreditLedgerEntryRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class BillingService {

    private static final int FREE_STARTING_CREDITS = 25;
    private static final int LOW_CREDIT_THRESHOLD = 5;
    private static final Duration STRIPE_TIMEOUT = Duration.ofSeconds(20);

    private final BillingSubscriptionRepository subscriptionRepository;
    private final CreditLedgerEntryRepository creditLedgerEntryRepository;
    private final AppUserRepository appUserRepository;
    private final AppProperties appProperties;
    private final ObjectMapper objectMapper;
    private final WebClient stripeClient;

    public BillingService(
        BillingSubscriptionRepository subscriptionRepository,
        CreditLedgerEntryRepository creditLedgerEntryRepository,
        AppUserRepository appUserRepository,
        AppProperties appProperties,
        ObjectMapper objectMapper,
        WebClient.Builder webClientBuilder
    ) {
        this.subscriptionRepository = subscriptionRepository;
        this.creditLedgerEntryRepository = creditLedgerEntryRepository;
        this.appUserRepository = appUserRepository;
        this.appProperties = appProperties;
        this.objectMapper = objectMapper;
        this.stripeClient = webClientBuilder
            .baseUrl(appProperties.getBilling().getStripeApiBaseUrl())
            .build();
    }

    public List<BillingPlanResponse> listPlans() {
        return List.of(
            new BillingPlanResponse("free", "Free", 25, 0, "Try viral faceless shorts"),
            new BillingPlanResponse("starter", "Starter", 300, 1900, "Solo creator and affiliate testing"),
            new BillingPlanResponse("pro", "Pro", 1200, 5900, "Daily publishing across channels"),
            new BillingPlanResponse("agency", "Agency", 5000, 19900, "Multi-channel client production")
        );
    }

    @Transactional
    public BillingSubscriptionResponse getSubscription(UUID userId) {
        return toSubscriptionResponse(ensureSubscription(userId));
    }

    @Transactional
    public BillingCreditsResponse getCredits(UUID userId) {
        BillingSubscription subscription = ensureSubscription(userId);
        return new BillingCreditsResponse(subscription.getCreditsBalance(), LOW_CREDIT_THRESHOLD);
    }

    @Transactional(readOnly = true)
    public List<BillingUsageEntryResponse> listUsage(UUID userId) {
        return creditLedgerEntryRepository.findTop25ByUserIdOrderByCreatedAtDesc(userId)
            .stream()
            .map(this::toUsageResponse)
            .toList();
    }

    @Transactional
    public BillingPortalResponse createCheckout(UUID userId, String requestedPlanKey) {
        BillingSubscription subscription = ensureSubscription(userId);
        String planKey = normalizePaidPlanKey(requestedPlanKey);
        String priceId = priceIdForPlan(planKey);
        AppUser user = appUserRepository.findById(userId)
            .orElseThrow(() -> new BadRequestException("User account not found"));
        String customerId = ensureStripeCustomer(subscription, user);

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("mode", "subscription");
        form.add("customer", customerId);
        form.add("line_items[0][price]", priceId);
        form.add("line_items[0][quantity]", "1");
        form.add("success_url", appProperties.getBilling().getSuccessUrl());
        form.add("cancel_url", appProperties.getBilling().getCancelUrl());
        form.add("client_reference_id", userId.toString());
        form.add("metadata[userId]", userId.toString());
        form.add("metadata[planKey]", planKey);
        form.add("subscription_data[metadata][userId]", userId.toString());
        form.add("subscription_data[metadata][planKey]", planKey);

        JsonNode response = postStripeForm("/v1/checkout/sessions", form);
        String url = textAt(response, "url");
        if (!StringUtils.hasText(url)) {
            throw new ExternalServiceException("Stripe checkout session did not return a URL");
        }
        return new BillingPortalResponse(url, "STRIPE_CHECKOUT");
    }

    @Transactional
    public BillingPortalResponse createPortal(UUID userId) {
        BillingSubscription subscription = ensureSubscription(userId);
        if (!StringUtils.hasText(subscription.getStripeCustomerId())) {
            throw new BadRequestException("Billing portal is available after the first Stripe checkout");
        }

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("customer", subscription.getStripeCustomerId());
        form.add("return_url", appProperties.getBilling().getPortalReturnUrl());

        JsonNode response = postStripeForm("/v1/billing_portal/sessions", form);
        String url = textAt(response, "url");
        if (!StringUtils.hasText(url)) {
            throw new ExternalServiceException("Stripe billing portal did not return a URL");
        }
        return new BillingPortalResponse(url, "STRIPE_PORTAL");
    }

    @Transactional
    public void handleStripeWebhook(String payload, String signatureHeader) {
        verifyStripeSignature(payload, signatureHeader);

        JsonNode event;
        try {
            event = objectMapper.readTree(payload);
        } catch (Exception ex) {
            throw new BadRequestException("Invalid Stripe webhook payload");
        }

        String eventId = textAt(event, "id");
        String eventType = textAt(event, "type");
        JsonNode object = event.path("data").path("object");

        if (!StringUtils.hasText(eventId) || !StringUtils.hasText(eventType) || object.isMissingNode()) {
            throw new BadRequestException("Invalid Stripe webhook event");
        }

        switch (eventType) {
            case "checkout.session.completed" -> handleCheckoutCompleted(object);
            case "customer.subscription.created", "customer.subscription.updated" -> handleSubscriptionChanged(object);
            case "customer.subscription.deleted" -> handleSubscriptionDeleted(object);
            case "invoice.paid" -> handleInvoicePaid(eventId, object);
            default -> {
                // Unknown or irrelevant Stripe events are acknowledged idempotently.
            }
        }
    }

    @Transactional(readOnly = true)
    public void assertHasCredits(UUID userId, int requiredCredits) {
        BillingSubscription subscription = subscriptionRepository.findByUserId(userId)
            .orElse(null);
        int balance = subscription == null ? FREE_STARTING_CREDITS : subscription.getCreditsBalance();
        if (balance < requiredCredits) {
            throw new BadRequestException("Not enough credits. Required: " + requiredCredits + ", available: " + balance);
        }
    }

    @Transactional
    public void debitCredits(UUID userId, int credits, String reason, UUID referenceId) {
        if (credits <= 0) {
            return;
        }
        BillingSubscription subscription = subscriptionRepository.findByUserIdForUpdate(userId)
            .orElseGet(() -> ensureSubscription(userId));
        if (subscription.getCreditsBalance() < credits) {
            throw new BadRequestException("Not enough credits. Required: " + credits + ", available: " + subscription.getCreditsBalance());
        }
        subscription.setCreditsBalance(subscription.getCreditsBalance() - credits);
        BillingSubscription saved = subscriptionRepository.save(subscription);
        writeLedger(userId, -credits, saved.getCreditsBalance(), reason, referenceId, null);
    }

    @Transactional
    public void creditCredits(UUID userId, int credits, String reason, UUID referenceId) {
        if (credits <= 0) {
            return;
        }
        BillingSubscription subscription = subscriptionRepository.findByUserIdForUpdate(userId)
            .orElseGet(() -> ensureSubscription(userId));
        subscription.setCreditsBalance(subscription.getCreditsBalance() + credits);
        BillingSubscription saved = subscriptionRepository.save(subscription);
        writeLedger(userId, credits, saved.getCreditsBalance(), reason, referenceId, null);
    }

    private void handleCheckoutCompleted(JsonNode session) {
        String customerId = textAt(session, "customer");
        String subscriptionId = textAt(session, "subscription");
        UUID userId = parseUserId(textAt(session.path("metadata"), "userId"));
        String planKey = normalizePlanKey(textAt(session.path("metadata"), "planKey"));
        if (userId == null || !StringUtils.hasText(customerId)) {
            return;
        }

        BillingSubscription subscription = ensureSubscription(userId);
        subscription.setStripeCustomerId(customerId);
        if (StringUtils.hasText(subscriptionId)) {
            subscription.setStripeSubscriptionId(subscriptionId);
        }
        if (isPaidPlan(planKey)) {
            subscription.setPlanKey(planKey);
        }
        subscription.setStatus("ACTIVE");
        subscriptionRepository.save(subscription);
    }

    private void handleSubscriptionChanged(JsonNode stripeSubscription) {
        BillingSubscription subscription = findSubscriptionFromStripeObject(stripeSubscription);
        if (subscription == null) {
            return;
        }
        String subscriptionId = textAt(stripeSubscription, "id");
        String customerId = textAt(stripeSubscription, "customer");
        String planKey = resolvePlanFromSubscription(stripeSubscription);
        if (StringUtils.hasText(subscriptionId)) {
            subscription.setStripeSubscriptionId(subscriptionId);
        }
        if (StringUtils.hasText(customerId)) {
            subscription.setStripeCustomerId(customerId);
        }
        if (isPaidPlan(planKey)) {
            subscription.setPlanKey(planKey);
        }
        subscription.setStatus(normalizeStripeStatus(textAt(stripeSubscription, "status")));
        subscriptionRepository.save(subscription);
    }

    private void handleSubscriptionDeleted(JsonNode stripeSubscription) {
        BillingSubscription subscription = findSubscriptionFromStripeObject(stripeSubscription);
        if (subscription == null) {
            return;
        }
        subscription.setStatus("CANCELED");
        subscription.setPlanKey("free");
        subscription.setStripeSubscriptionId(textAt(stripeSubscription, "id"));
        subscriptionRepository.save(subscription);
    }

    private void handleInvoicePaid(String eventId, JsonNode invoice) {
        if (creditLedgerEntryRepository.existsByStripeEventId(eventId)) {
            return;
        }

        BillingSubscription subscription = findSubscriptionFromStripeObject(invoice);
        if (subscription == null) {
            return;
        }

        String planKey = resolvePlanFromInvoice(invoice);
        if (!isPaidPlan(planKey)) {
            planKey = subscription.getPlanKey();
        }
        int monthlyCredits = monthlyCreditsForPlan(planKey);
        if (monthlyCredits <= 0) {
            return;
        }

        subscription.setPlanKey(planKey);
        subscription.setStatus("ACTIVE");
        subscription.setCreditsBalance(subscription.getCreditsBalance() + monthlyCredits);
        BillingSubscription saved = subscriptionRepository.save(subscription);
        writeLedger(saved.getUserId(), monthlyCredits, saved.getCreditsBalance(), "stripe_invoice_paid", null, eventId);
    }

    private BillingSubscription findSubscriptionFromStripeObject(JsonNode object) {
        String customerId = textAt(object, "customer");
        String subscriptionId = textAt(object, "subscription");
        if (!StringUtils.hasText(subscriptionId)) {
            subscriptionId = textAt(object, "id");
        }

        if (StringUtils.hasText(subscriptionId)) {
            BillingSubscription bySubscription = subscriptionRepository.findByStripeSubscriptionId(subscriptionId).orElse(null);
            if (bySubscription != null) {
                return bySubscription;
            }
        }
        if (StringUtils.hasText(customerId)) {
            return subscriptionRepository.findByStripeCustomerId(customerId).orElse(null);
        }
        return null;
    }

    private BillingSubscription ensureSubscription(UUID userId) {
        return subscriptionRepository.findByUserId(userId)
            .orElseGet(() -> {
                BillingSubscription subscription = new BillingSubscription();
                subscription.setUserId(userId);
                subscription.setPlanKey("free");
                subscription.setStatus("ACTIVE");
                subscription.setCreditsBalance(FREE_STARTING_CREDITS);
                BillingSubscription saved = subscriptionRepository.save(subscription);
                writeLedger(userId, FREE_STARTING_CREDITS, FREE_STARTING_CREDITS, "free_signup_grant", null, null);
                return saved;
            });
    }

    private String ensureStripeCustomer(BillingSubscription subscription, AppUser user) {
        requireStripeConfigured();
        if (StringUtils.hasText(subscription.getStripeCustomerId())) {
            return subscription.getStripeCustomerId();
        }

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("email", user.getEmail());
        form.add("name", user.getDisplayName());
        form.add("metadata[userId]", user.getId().toString());

        JsonNode response = postStripeForm("/v1/customers", form);
        String customerId = textAt(response, "id");
        if (!StringUtils.hasText(customerId)) {
            throw new ExternalServiceException("Stripe customer create did not return an id");
        }

        subscription.setStripeCustomerId(customerId);
        subscriptionRepository.save(subscription);
        return customerId;
    }

    private JsonNode postStripeForm(String path, MultiValueMap<String, String> form) {
        requireStripeConfigured();
        try {
            String body = stripeClient.post()
                .uri(path)
                .headers(headers -> headers.setBearerAuth(appProperties.getBilling().getStripeSecretKey()))
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData(form))
                .retrieve()
                .bodyToMono(String.class)
                .block(STRIPE_TIMEOUT);
            return objectMapper.readTree(body);
        } catch (Exception ex) {
            throw new ExternalServiceException("Stripe request failed: " + ex.getMessage(), ex);
        }
    }

    private void verifyStripeSignature(String payload, String signatureHeader) {
        String secret = appProperties.getBilling().getStripeWebhookSecret();
        if (!StringUtils.hasText(secret)) {
            throw new BadRequestException("Stripe webhook secret is not configured");
        }
        if (!StringUtils.hasText(signatureHeader)) {
            throw new BadRequestException("Missing Stripe signature");
        }

        String timestamp = null;
        String expectedSignature = null;
        for (String part : signatureHeader.split(",")) {
            String[] pair = part.split("=", 2);
            if (pair.length != 2) {
                continue;
            }
            if ("t".equals(pair[0])) {
                timestamp = pair[1];
            } else if ("v1".equals(pair[0])) {
                expectedSignature = pair[1];
            }
        }
        if (!StringUtils.hasText(timestamp) || !StringUtils.hasText(expectedSignature)) {
            throw new BadRequestException("Invalid Stripe signature header");
        }

        String signedPayload = timestamp + "." + payload;
        String actualSignature = hmacSha256(secret, signedPayload);
        if (!MessageDigest.isEqual(
            actualSignature.getBytes(StandardCharsets.UTF_8),
            expectedSignature.getBytes(StandardCharsets.UTF_8)
        )) {
            throw new BadRequestException("Invalid Stripe signature");
        }
    }

    private String hmacSha256(String secret, String payload) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new IllegalStateException("Could not verify Stripe signature", ex);
        }
    }

    private void requireStripeConfigured() {
        if (!appProperties.getBilling().isStripeEnabled()) {
            throw new BadRequestException("Stripe billing is not enabled");
        }
        if (!StringUtils.hasText(appProperties.getBilling().getStripeSecretKey())) {
            throw new BadRequestException("STRIPE_SECRET_KEY is required for Stripe billing");
        }
    }

    private String normalizePaidPlanKey(String requestedPlanKey) {
        String planKey = normalizePlanKey(requestedPlanKey);
        if (!isPaidPlan(planKey)) {
            throw new BadRequestException("Choose a paid plan for checkout");
        }
        priceIdForPlan(planKey);
        return planKey;
    }

    private String normalizePlanKey(String requestedPlanKey) {
        return StringUtils.hasText(requestedPlanKey)
            ? requestedPlanKey.trim().toLowerCase(Locale.ROOT)
            : "starter";
    }

    private boolean isPaidPlan(String planKey) {
        return "starter".equals(planKey) || "pro".equals(planKey) || "agency".equals(planKey);
    }

    private String priceIdForPlan(String planKey) {
        String priceId = switch (planKey) {
            case "starter" -> appProperties.getBilling().getStarterPriceId();
            case "pro" -> appProperties.getBilling().getProPriceId();
            case "agency" -> appProperties.getBilling().getAgencyPriceId();
            default -> null;
        };
        if (!StringUtils.hasText(priceId)) {
            throw new BadRequestException("Stripe price id is not configured for plan: " + planKey);
        }
        return priceId;
    }

    private String planKeyForPriceId(String priceId) {
        if (!StringUtils.hasText(priceId)) {
            return null;
        }
        if (priceId.equals(appProperties.getBilling().getStarterPriceId())) {
            return "starter";
        }
        if (priceId.equals(appProperties.getBilling().getProPriceId())) {
            return "pro";
        }
        if (priceId.equals(appProperties.getBilling().getAgencyPriceId())) {
            return "agency";
        }
        return null;
    }

    private int monthlyCreditsForPlan(String planKey) {
        return listPlans().stream()
            .filter(plan -> plan.getPlanKey().equals(planKey))
            .map(BillingPlanResponse::getMonthlyCredits)
            .findFirst()
            .orElse(0);
    }

    private String resolvePlanFromSubscription(JsonNode subscription) {
        JsonNode items = subscription.path("items").path("data");
        if (items.isArray() && !items.isEmpty()) {
            return planKeyForPriceId(textAt(items.get(0).path("price"), "id"));
        }
        return normalizePlanKey(textAt(subscription.path("metadata"), "planKey"));
    }

    private String resolvePlanFromInvoice(JsonNode invoice) {
        JsonNode lines = invoice.path("lines").path("data");
        if (lines.isArray() && !lines.isEmpty()) {
            return planKeyForPriceId(textAt(lines.get(0).path("price"), "id"));
        }
        return null;
    }

    private String normalizeStripeStatus(String status) {
        if (!StringUtils.hasText(status)) {
            return "UNKNOWN";
        }
        return status.trim().toUpperCase(Locale.ROOT);
    }

    private UUID parseUserId(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private String textAt(JsonNode node, String fieldName) {
        if (node == null || node.path(fieldName).isMissingNode() || node.path(fieldName).isNull()) {
            return null;
        }
        return node.path(fieldName).asText();
    }

    private void writeLedger(UUID userId, int amount, int balanceAfter, String reason, UUID referenceId, String stripeEventId) {
        CreditLedgerEntry entry = new CreditLedgerEntry();
        entry.setUserId(userId);
        entry.setAmount(amount);
        entry.setBalanceAfter(balanceAfter);
        entry.setReason(reason);
        entry.setReferenceId(referenceId);
        entry.setStripeEventId(stripeEventId);
        creditLedgerEntryRepository.save(entry);
    }

    private BillingSubscriptionResponse toSubscriptionResponse(BillingSubscription subscription) {
        BillingSubscriptionResponse response = new BillingSubscriptionResponse();
        response.setPlanKey(subscription.getPlanKey());
        response.setStatus(subscription.getStatus());
        response.setCreditsBalance(subscription.getCreditsBalance());
        response.setUpdatedAt(subscription.getUpdatedAt());
        return response;
    }

    private BillingUsageEntryResponse toUsageResponse(CreditLedgerEntry entry) {
        BillingUsageEntryResponse response = new BillingUsageEntryResponse();
        response.setId(entry.getId());
        response.setAmount(entry.getAmount());
        response.setBalanceAfter(entry.getBalanceAfter());
        response.setReason(entry.getReason());
        response.setReferenceId(entry.getReferenceId());
        response.setCreatedAt(entry.getCreatedAt());
        return response;
    }
}
