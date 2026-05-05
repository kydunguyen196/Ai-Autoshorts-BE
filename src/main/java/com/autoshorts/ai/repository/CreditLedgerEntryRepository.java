package com.autoshorts.ai.repository;

import com.autoshorts.ai.entity.CreditLedgerEntry;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface CreditLedgerEntryRepository extends JpaRepository<CreditLedgerEntry, UUID> {

    List<CreditLedgerEntry> findTop25ByUserIdOrderByCreatedAtDesc(UUID userId);

    boolean existsByStripeEventId(String stripeEventId);
}
