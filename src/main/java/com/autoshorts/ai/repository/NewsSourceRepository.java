package com.autoshorts.ai.repository;

import com.autoshorts.ai.entity.NewsSource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface NewsSourceRepository extends JpaRepository<NewsSource, UUID> {

    List<NewsSource> findAllByOwnerUserIdOrderByCreatedAtDesc(UUID ownerUserId);

    Page<NewsSource> findAllByOwnerUserId(UUID ownerUserId, Pageable pageable);

    Optional<NewsSource> findByIdAndOwnerUserId(UUID id, UUID ownerUserId);

    List<NewsSource> findAllByEnabledTrue();

    long countByEnabledTrue();
}
