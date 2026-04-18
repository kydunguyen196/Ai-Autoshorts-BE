package com.autoshorts.ai.repository;

import com.autoshorts.ai.entity.CharacterCampaign;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CharacterCampaignRepository extends JpaRepository<CharacterCampaign, UUID> {

    List<CharacterCampaign> findAllByOwnerUserIdOrderByCreatedAtDesc(UUID ownerUserId);

    List<CharacterCampaign> findAllByOwnerUserIdAndChannelIdOrderByCreatedAtDesc(UUID ownerUserId, UUID channelId);

    Optional<CharacterCampaign> findByIdAndOwnerUserId(UUID id, UUID ownerUserId);
}
