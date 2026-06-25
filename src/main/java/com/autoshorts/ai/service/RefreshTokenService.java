package com.autoshorts.ai.service;

import com.autoshorts.ai.config.SecurityProperties;
import com.autoshorts.ai.entity.RefreshToken;
import com.autoshorts.ai.exception.BadRequestException;
import com.autoshorts.ai.repository.RefreshTokenRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;

/**
 * Issues and rotates opaque refresh tokens. The raw token is returned to the client once;
 * only its SHA-256 hash is persisted, so a database leak cannot be replayed.
 */
@Service
public class RefreshTokenService {

    private static final Logger log = LoggerFactory.getLogger(RefreshTokenService.class);

    private final RefreshTokenRepository refreshTokenRepository;
    private final SecurityProperties securityProperties;
    private final SecureRandom secureRandom = new SecureRandom();

    public RefreshTokenService(
        RefreshTokenRepository refreshTokenRepository,
        SecurityProperties securityProperties
    ) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.securityProperties = securityProperties;
    }

    /** Creates a new refresh token for the user and returns the raw (un-hashed) value. */
    @Transactional
    public String issue(UUID userId) {
        String rawToken = generateRawToken();
        RefreshToken entity = new RefreshToken();
        entity.setUserId(userId);
        entity.setTokenHash(hash(rawToken));
        Instant now = Instant.now();
        entity.setIssuedAt(now);
        entity.setExpiresAt(now.plusSeconds(securityProperties.getRefreshTokenTtlSeconds()));
        refreshTokenRepository.save(entity);
        return rawToken;
    }

    /**
     * Validates a refresh token, revokes it (single-use rotation), and issues a fresh one.
     * Returns the owning userId plus the new raw token.
     */
    @Transactional
    public RotationResult rotate(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            throw new BadRequestException("Refresh token is required");
        }
        RefreshToken existing = refreshTokenRepository.findByTokenHash(hash(rawToken))
            .orElseThrow(() -> new BadRequestException("Invalid refresh token"));

        Instant now = Instant.now();
        if (!existing.isActive(now)) {
            // Token reuse or expiry: revoke every token for the user as a precaution.
            refreshTokenRepository.revokeAllForUser(existing.getUserId(), now);
            log.warn("event=refresh_token_rejected userId={} reason=inactive", existing.getUserId());
            throw new BadRequestException("Refresh token is no longer valid");
        }

        existing.setRevokedAt(now);
        refreshTokenRepository.save(existing);
        String newRaw = issue(existing.getUserId());
        return new RotationResult(existing.getUserId(), newRaw);
    }

    @Transactional
    public void revoke(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            return;
        }
        refreshTokenRepository.findByTokenHash(hash(rawToken)).ifPresent(token -> {
            if (token.getRevokedAt() == null) {
                token.setRevokedAt(Instant.now());
                refreshTokenRepository.save(token);
            }
        });
    }

    @Transactional
    public void revokeAllForUser(UUID userId) {
        refreshTokenRepository.revokeAllForUser(userId, Instant.now());
    }

    private String generateRawToken() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hash(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashed);
        } catch (Exception ex) {
            throw new IllegalStateException("SHA-256 not available", ex);
        }
    }

    public record RotationResult(UUID userId, String rawToken) {
    }
}
