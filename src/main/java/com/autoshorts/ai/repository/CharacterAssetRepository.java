package com.autoshorts.ai.repository;

import com.autoshorts.ai.entity.CharacterAsset;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface CharacterAssetRepository extends JpaRepository<CharacterAsset, UUID> {

    List<CharacterAsset> findAllByOwnerUserIdAndCharacterProfileIdOrderByCreatedAtDesc(UUID ownerUserId, UUID characterProfileId);
}
