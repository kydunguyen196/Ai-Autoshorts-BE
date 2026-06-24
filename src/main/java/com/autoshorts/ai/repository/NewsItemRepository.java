package com.autoshorts.ai.repository;

import com.autoshorts.ai.entity.NewsItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface NewsItemRepository extends JpaRepository<NewsItem, UUID> {

    boolean existsByNewsSourceIdAndExternalGuid(UUID newsSourceId, String externalGuid);

    Page<NewsItem> findAllByNewsSourceIdOrderByCreatedAtDesc(UUID newsSourceId, Pageable pageable);

    Page<NewsItem> findAllByNewsSourceIdInOrderByCreatedAtDesc(java.util.Collection<UUID> sourceIds, Pageable pageable);

    long countByNewsSourceId(UUID newsSourceId);
}
