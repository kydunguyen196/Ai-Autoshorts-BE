package com.autoshorts.ai.repository;

import com.autoshorts.ai.entity.WebhookDelivery;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface WebhookDeliveryRepository extends JpaRepository<WebhookDelivery, UUID> {

    @Query(value = """
        SELECT *
        FROM webhook_deliveries
        WHERE status = 'PENDING'
          AND next_attempt_at <= :now
        ORDER BY next_attempt_at ASC, created_at ASC
        LIMIT :limit
        FOR UPDATE SKIP LOCKED
        """, nativeQuery = true)
    List<WebhookDelivery> findDueDeliveriesForUpdate(@Param("now") Instant now, @Param("limit") int limit);

    List<WebhookDelivery> findAllByJobIdOrderByCreatedAtDesc(UUID jobId);
}
