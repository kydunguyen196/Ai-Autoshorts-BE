package com.autoshorts.ai.service;

import com.autoshorts.ai.entity.AdminAuditLog;
import com.autoshorts.ai.repository.AdminAuditLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/** Records admin actions for accountability. Best-effort: never blocks the action it audits. */
@Service
public class AuditService {

    private static final Logger log = LoggerFactory.getLogger(AuditService.class);

    private final AdminAuditLogRepository repository;

    public AuditService(AdminAuditLogRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public void record(UUID actorUserId, String actorEmail, String action, String targetType, String targetId, String details) {
        try {
            AdminAuditLog entry = new AdminAuditLog();
            entry.setActorUserId(actorUserId);
            entry.setActorEmail(actorEmail);
            entry.setAction(action);
            entry.setTargetType(targetType);
            entry.setTargetId(targetId);
            entry.setDetails(details);
            repository.save(entry);
        } catch (Exception ex) {
            log.warn("event=audit_record_failed action={} message={}", action, ex.getMessage());
        }
    }
}
