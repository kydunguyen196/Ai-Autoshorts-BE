package com.autoshorts.ai.repository;

import com.autoshorts.ai.entity.Channel;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ChannelRepository extends JpaRepository<Channel, UUID> {

    List<Channel> findAllByUserIdOrderByCreatedAtDesc(UUID userId);

    Optional<Channel> findByIdAndUserId(UUID channelId, UUID userId);

    Optional<Channel> findFirstByUserIdAndIsDefaultTrue(UUID userId);

    boolean existsByUserIdAndNameIgnoreCase(UUID userId, String name);
}
