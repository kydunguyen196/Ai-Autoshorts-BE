package com.autoshorts.ai.repository;

import com.autoshorts.ai.entity.BillingSubscription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;

public interface BillingSubscriptionRepository extends JpaRepository<BillingSubscription, UUID> {

    Optional<BillingSubscription> findByUserId(UUID userId);

    Optional<BillingSubscription> findByStripeCustomerId(String stripeCustomerId);

    Optional<BillingSubscription> findByStripeSubscriptionId(String stripeSubscriptionId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select subscription from BillingSubscription subscription where subscription.userId = :userId")
    Optional<BillingSubscription> findByUserIdForUpdate(@Param("userId") UUID userId);
}
