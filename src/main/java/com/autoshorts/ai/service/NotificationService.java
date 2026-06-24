package com.autoshorts.ai.service;

import com.autoshorts.ai.dto.NotificationResponse;
import com.autoshorts.ai.entity.AppUser;
import com.autoshorts.ai.entity.Notification;
import com.autoshorts.ai.entity.NotificationType;
import com.autoshorts.ai.exception.ResourceNotFoundException;
import com.autoshorts.ai.repository.AppUserRepository;
import com.autoshorts.ai.repository.NotificationRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Central entry point for in-app notifications. Persists each notification, pushes it to
 * the user's live SSE connections (if any), and optionally emails the user for the
 * notification types that warrant it.
 */
@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    /** Types that also trigger an email when mail is enabled. */
    private static final Set<NotificationType> EMAIL_TYPES = EnumSet.of(
        NotificationType.JOB_FAILED,
        NotificationType.PUBLISH_FAILED,
        NotificationType.CREDIT_LOW
    );

    private final NotificationRepository repository;
    private final NotificationSseService sseService;
    private final EmailService emailService;
    private final AppUserRepository appUserRepository;
    private final ObjectMapper objectMapper;

    public NotificationService(
        NotificationRepository repository,
        NotificationSseService sseService,
        EmailService emailService,
        AppUserRepository appUserRepository,
        ObjectMapper objectMapper
    ) {
        this.repository = repository;
        this.sseService = sseService;
        this.emailService = emailService;
        this.appUserRepository = appUserRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public Notification notify(UUID userId, NotificationType type, String title, String message, Map<String, ?> data) {
        Notification notification = new Notification();
        notification.setUserId(userId);
        notification.setType(type);
        notification.setTitle(title);
        notification.setMessage(message);
        notification.setDataJson(writeData(data));
        Notification saved = repository.save(notification);

        // Real-time push (best-effort)
        try {
            sseService.push(userId, "notification", NotificationResponse.from(saved));
        } catch (Exception ex) {
            log.debug("event=notification_sse_push_failed userId={} message={}", userId, ex.getMessage());
        }

        // Email for important types
        if (EMAIL_TYPES.contains(type)) {
            appUserRepository.findById(userId).ifPresent(user ->
                emailService.sendText(user.getEmail(), title, message == null ? title : message));
        }

        log.info("event=notification_created userId={} type={}", userId, type);
        return saved;
    }

    /** Sends a broadcast notification to every enabled user. */
    @Transactional
    public int broadcast(String title, String message, Map<String, ?> data) {
        int count = 0;
        for (AppUser user : appUserRepository.findAll()) {
            if (!user.isEnabled()) {
                continue;
            }
            notify(user.getId(), NotificationType.ADMIN_BROADCAST, title, message, data);
            count++;
        }
        return count;
    }

    @Transactional(readOnly = true)
    public Page<NotificationResponse> list(UUID userId, int page, int size, boolean unreadOnly) {
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.min(Math.max(1, size), 100));
        Page<Notification> result = unreadOnly
            ? repository.findAllByUserIdAndReadAtIsNullOrderByCreatedAtDesc(userId, pageable)
            : repository.findAllByUserIdOrderByCreatedAtDesc(userId, pageable);
        return result.map(NotificationResponse::from);
    }

    @Transactional(readOnly = true)
    public long unreadCount(UUID userId) {
        return repository.countByUserIdAndReadAtIsNull(userId);
    }

    @Transactional
    public void markRead(UUID userId, UUID notificationId) {
        Notification notification = repository.findByIdAndUserId(notificationId, userId)
            .orElseThrow(() -> new ResourceNotFoundException("Notification not found: " + notificationId));
        if (notification.getReadAt() == null) {
            notification.setReadAt(Instant.now());
            repository.save(notification);
        }
    }

    @Transactional
    public int markAllRead(UUID userId) {
        return repository.markAllRead(userId, Instant.now());
    }

    private String writeData(Map<String, ?> data) {
        if (data == null || data.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(data);
        } catch (Exception ex) {
            return null;
        }
    }
}
