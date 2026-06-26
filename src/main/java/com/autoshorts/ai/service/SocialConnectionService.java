package com.autoshorts.ai.service;

import com.autoshorts.ai.dto.SocialConnectionStatusResponse;
import com.autoshorts.ai.dto.SocialConnectionUpsertRequest;
import com.autoshorts.ai.entity.SocialAccountConnection;
import com.autoshorts.ai.entity.SocialConnectionStatus;
import com.autoshorts.ai.entity.SocialPlatform;
import com.autoshorts.ai.repository.SocialAccountConnectionRepository;
import com.autoshorts.ai.security.TokenCipher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Manages user connections to YouTube / Instagram (generic counterpart of
 * {@link TikTokConnectionService}). Tokens are encrypted at rest via {@link TokenCipher}.
 */
@Service
public class SocialConnectionService {

    private final SocialAccountConnectionRepository repository;
    private final ChannelService channelService;
    private final TokenCipher tokenCipher;

    public SocialConnectionService(
        SocialAccountConnectionRepository repository,
        ChannelService channelService,
        TokenCipher tokenCipher
    ) {
        this.repository = repository;
        this.channelService = channelService;
        this.tokenCipher = tokenCipher;
    }

    @Transactional
    public SocialConnectionStatusResponse upsertConnection(
        UUID userId, SocialPlatform platform, SocialConnectionUpsertRequest request
    ) {
        UUID channelId = channelService.resolveOwnedChannelIdOrDefault(userId, request.getChannelId());
        SocialAccountConnection connection = repository
            .findByOwnerUserIdAndChannelIdAndPlatform(userId, channelId, platform)
            .orElseGet(SocialAccountConnection::new);

        connection.setOwnerUserId(userId);
        connection.setChannelId(channelId);
        connection.setPlatform(platform);
        connection.setPlatformAccountId(trimToNull(request.getPlatformAccountId()));
        connection.setPlatformUsername(trimToNull(request.getPlatformUsername()));
        if (request.getAccessToken() != null) {
            connection.setAccessTokenEncrypted(encodeToken(request.getAccessToken()));
        }
        if (request.getRefreshToken() != null) {
            connection.setRefreshTokenEncrypted(encodeToken(request.getRefreshToken()));
        }
        connection.setTokenExpiresAt(request.getTokenExpiresAt());
        connection.setScopes(joinScopes(request.getScopes()));
        connection.setStatus(request.getStatus() == null ? SocialConnectionStatus.ACTIVE : request.getStatus());
        connection.setLastSyncAt(Instant.now());

        return toResponse(repository.save(connection));
    }

    @Transactional(readOnly = true)
    public SocialConnectionStatusResponse getConnectionStatus(UUID userId, SocialPlatform platform, UUID channelId) {
        UUID resolvedChannelId = channelService.resolveOwnedChannelIdOrDefault(userId, channelId);
        return repository.findByOwnerUserIdAndChannelIdAndPlatform(userId, resolvedChannelId, platform)
            .map(this::toResponse)
            .orElseGet(() -> {
                SocialConnectionStatusResponse response = new SocialConnectionStatusResponse();
                response.setChannelId(resolvedChannelId);
                response.setPlatform(platform);
                response.setStatus(SocialConnectionStatus.PENDING);
                response.setActive(false);
                response.setScopes(List.of());
                return response;
            });
    }

    @Transactional(readOnly = true)
    public Optional<SocialAccountConnection> findActiveConnection(UUID userId, UUID channelId, SocialPlatform platform) {
        Instant now = Instant.now();
        return repository
            .findByOwnerUserIdAndChannelIdAndPlatformAndStatus(userId, channelId, platform, SocialConnectionStatus.ACTIVE)
            .filter(connection -> connection.getTokenExpiresAt() == null || connection.getTokenExpiresAt().isAfter(now));
    }

    @Transactional(readOnly = true)
    public String describeConnectionState(UUID userId, UUID channelId, SocialPlatform platform) {
        Optional<SocialAccountConnection> connection =
            repository.findByOwnerUserIdAndChannelIdAndPlatform(userId, channelId, platform);
        if (connection.isEmpty()) {
            return "MISSING";
        }

        SocialAccountConnection existing = connection.get();
        if (existing.getStatus() != SocialConnectionStatus.ACTIVE) {
            return existing.getStatus().name();
        }
        if (existing.getTokenExpiresAt() != null && !existing.getTokenExpiresAt().isAfter(Instant.now())) {
            return "EXPIRED";
        }
        return "ACTIVE";
    }

    /** Decrypted access token for a platform connection (used by the direct-publish API clients). */
    @Transactional(readOnly = true)
    public Optional<String> findAccessToken(UUID userId, UUID channelId, SocialPlatform platform) {
        return findActiveConnection(userId, channelId, platform)
            .map(SocialAccountConnection::getAccessTokenEncrypted)
            .filter(StringUtils::hasText)
            .map(tokenCipher::decrypt);
    }

    private SocialConnectionStatusResponse toResponse(SocialAccountConnection connection) {
        SocialConnectionStatusResponse response = new SocialConnectionStatusResponse();
        response.setId(connection.getId());
        response.setChannelId(connection.getChannelId());
        response.setPlatform(connection.getPlatform());
        response.setPlatformAccountId(connection.getPlatformAccountId());
        response.setPlatformUsername(connection.getPlatformUsername());
        response.setTokenExpiresAt(connection.getTokenExpiresAt());
        response.setScopes(splitScopes(connection.getScopes()));
        response.setStatus(connection.getStatus());
        response.setActive(isActive(connection));
        response.setLastSyncAt(connection.getLastSyncAt());
        response.setCreatedAt(connection.getCreatedAt());
        response.setUpdatedAt(connection.getUpdatedAt());
        return response;
    }

    private boolean isActive(SocialAccountConnection connection) {
        return connection.getStatus() == SocialConnectionStatus.ACTIVE
            && (connection.getTokenExpiresAt() == null || connection.getTokenExpiresAt().isAfter(Instant.now()));
    }

    private String joinScopes(List<String> scopes) {
        if (scopes == null || scopes.isEmpty()) {
            return null;
        }
        return String.join(",", scopes.stream().filter(StringUtils::hasText).map(String::trim).toList());
    }

    private List<String> splitScopes(String scopes) {
        if (!StringUtils.hasText(scopes)) {
            return List.of();
        }
        return Arrays.stream(scopes.split(","))
            .map(String::trim)
            .filter(StringUtils::hasText)
            .toList();
    }

    private String encodeToken(String rawToken) {
        if (!StringUtils.hasText(rawToken)) {
            return null;
        }
        return tokenCipher.encrypt(rawToken.trim());
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }
}
