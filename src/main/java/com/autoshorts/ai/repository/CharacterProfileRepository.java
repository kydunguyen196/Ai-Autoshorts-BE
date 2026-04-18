package com.autoshorts.ai.repository;

import com.autoshorts.ai.entity.CharacterProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CharacterProfileRepository extends JpaRepository<CharacterProfile, UUID> {

    List<CharacterProfile> findAllByOwnerUserIdOrderByCreatedAtDesc(UUID ownerUserId);

    List<CharacterProfile> findAllByOwnerUserIdAndChannelIdOrderByCreatedAtDesc(UUID ownerUserId, UUID channelId);

    Optional<CharacterProfile> findByIdAndOwnerUserId(UUID id, UUID ownerUserId);
}
