package com.autoshorts.ai.repository;

import com.autoshorts.ai.entity.VideoTemplate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface VideoTemplateRepository extends JpaRepository<VideoTemplate, UUID> {

    List<VideoTemplate> findByOwnerUserIdOrderByCreatedAtDesc(UUID ownerUserId);

    Optional<VideoTemplate> findByIdAndOwnerUserId(UUID id, UUID ownerUserId);

    long countByOwnerUserId(UUID ownerUserId);
}
