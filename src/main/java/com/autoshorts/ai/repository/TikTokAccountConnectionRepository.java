package com.autoshorts.ai.repository;

import com.autoshorts.ai.entity.TikTokAccountConnection;
import com.autoshorts.ai.entity.TikTokConnectionStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface TikTokAccountConnectionRepository extends JpaRepository<TikTokAccountConnection, UUID> {

    Optional<TikTokAccountConnection> findByOwnerUserIdAndChannelId(UUID ownerUserId, UUID channelId);

    Optional<TikTokAccountConnection> findByOwnerUserIdAndChannelIdAndStatus(UUID ownerUserId, UUID channelId, TikTokConnectionStatus status);

    default boolean hasActiveConnection(UUID ownerUserId, UUID channelId, Instant now) {
        return findByOwnerUserIdAndChannelIdAndStatus(ownerUserId, channelId, TikTokConnectionStatus.ACTIVE)
            .filter(connection -> connection.getTokenExpiresAt() == null || connection.getTokenExpiresAt().isAfter(now))
            .isPresent();
    }
}
