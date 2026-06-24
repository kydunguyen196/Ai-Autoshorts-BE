package com.autoshorts.ai.service;

import com.autoshorts.ai.dto.TikTokConnectionStatusResponse;
import com.autoshorts.ai.dto.TikTokConnectionUpsertRequest;
import com.autoshorts.ai.entity.TikTokAccountConnection;
import com.autoshorts.ai.entity.TikTokConnectionStatus;
import com.autoshorts.ai.repository.TikTokAccountConnectionRepository;
import com.autoshorts.ai.security.TokenCipher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class TikTokConnectionService {

    private final TikTokAccountConnectionRepository repository;
    private final ChannelService channelService;
    private final TokenCipher tokenCipher;

    public TikTokConnectionService(
        TikTokAccountConnectionRepository repository,
        ChannelService channelService,
        TokenCipher tokenCipher
    ) {
        this.repository = repository;
        this.channelService = channelService;
        this.tokenCipher = tokenCipher;
    }

    @Transactional
    public TikTokConnectionStatusResponse upsertConnection(UUID userId, TikTokConnectionUpsertRequest request) {
        UUID channelId = channelService.resolveOwnedChannelIdOrDefault(userId, request.getChannelId());
        TikTokAccountConnection connection = repository.findByOwnerUserIdAndChannelId(userId, channelId)
            .orElseGet(TikTokAccountConnection::new);

        connection.setOwnerUserId(userId);
        connection.setChannelId(channelId);
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
        connection.setStatus(request.getStatus() == null ? TikTokConnectionStatus.ACTIVE : request.getStatus());
        connection.setLastSyncAt(Instant.now());

        return toResponse(repository.save(connection));
    }

    @Transactional(readOnly = true)
    public TikTokConnectionStatusResponse getConnectionStatus(UUID userId, UUID channelId) {
        UUID resolvedChannelId = channelService.resolveOwnedChannelIdOrDefault(userId, channelId);
        return repository.findByOwnerUserIdAndChannelId(userId, resolvedChannelId)
            .map(this::toResponse)
            .orElseGet(() -> {
                TikTokConnectionStatusResponse response = new TikTokConnectionStatusResponse();
                response.setChannelId(resolvedChannelId);
                response.setStatus(TikTokConnectionStatus.PENDING);
                response.setActive(false);
                response.setScopes(List.of());
                return response;
            });
    }

    @Transactional(readOnly = true)
    public Optional<TikTokAccountConnection> findActiveConnection(UUID userId, UUID channelId) {
        Instant now = Instant.now();
        return repository.findByOwnerUserIdAndChannelIdAndStatus(userId, channelId, TikTokConnectionStatus.ACTIVE)
            .filter(connection -> connection.getTokenExpiresAt() == null || connection.getTokenExpiresAt().isAfter(now));
    }

    @Transactional(readOnly = true)
    public String describeConnectionState(UUID userId, UUID channelId) {
        Optional<TikTokAccountConnection> connection = repository.findByOwnerUserIdAndChannelId(userId, channelId);
        if (connection.isEmpty()) {
            return "MISSING";
        }

        TikTokAccountConnection existing = connection.get();
        if (existing.getStatus() != TikTokConnectionStatus.ACTIVE) {
            return existing.getStatus().name();
        }
        if (existing.getTokenExpiresAt() != null && !existing.getTokenExpiresAt().isAfter(Instant.now())) {
            return "EXPIRED";
        }
        return "ACTIVE";
    }

    private TikTokConnectionStatusResponse toResponse(TikTokAccountConnection connection) {
        TikTokConnectionStatusResponse response = new TikTokConnectionStatusResponse();
        response.setId(connection.getId());
        response.setChannelId(connection.getChannelId());
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

    private boolean isActive(TikTokAccountConnection connection) {
        return connection.getStatus() == TikTokConnectionStatus.ACTIVE
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
