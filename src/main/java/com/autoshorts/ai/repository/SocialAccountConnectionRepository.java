package com.autoshorts.ai.repository;

import com.autoshorts.ai.entity.SocialAccountConnection;
import com.autoshorts.ai.entity.SocialConnectionStatus;
import com.autoshorts.ai.entity.SocialPlatform;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface SocialAccountConnectionRepository extends JpaRepository<SocialAccountConnection, UUID> {

    Optional<SocialAccountConnection> findByOwnerUserIdAndChannelIdAndPlatform(
        UUID ownerUserId, UUID channelId, SocialPlatform platform);

    Optional<SocialAccountConnection> findByOwnerUserIdAndChannelIdAndPlatformAndStatus(
        UUID ownerUserId, UUID channelId, SocialPlatform platform, SocialConnectionStatus status);

    default boolean hasActiveConnection(UUID ownerUserId, UUID channelId, SocialPlatform platform, Instant now) {
        return findByOwnerUserIdAndChannelIdAndPlatformAndStatus(ownerUserId, channelId, platform, SocialConnectionStatus.ACTIVE)
            .filter(connection -> connection.getTokenExpiresAt() == null || connection.getTokenExpiresAt().isAfter(now))
            .isPresent();
    }
}
