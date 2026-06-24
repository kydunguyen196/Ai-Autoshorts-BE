package com.autoshorts.ai.security;

import com.autoshorts.ai.config.SecurityProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

/**
 * Signs and verifies the OAuth {@code state} parameter for the TikTok connect flow.
 *
 * <p>The state is a short-lived (10 min) signed JWT that carries the initiating user and channel.
 * The callback arrives on the browser without our app JWT, so the signed state both prevents CSRF
 * and lets us attribute the returned authorization code to the correct account.
 */
@Service
public class TikTokOAuthStateService {

    private static final long STATE_TTL_SECONDS = 600;

    private final SecretKey signingKey;

    public TikTokOAuthStateService(SecurityProperties securityProperties) {
        byte[] keyBytes = securityProperties.getJwtSecret().getBytes(StandardCharsets.UTF_8);
        this.signingKey = Keys.hmacShaKeyFor(keyBytes);
    }

    public String createState(UUID userId, UUID channelId) {
        Instant now = Instant.now();
        return Jwts.builder()
            .subject(userId.toString())
            .claim("channelId", channelId.toString())
            .claim("purpose", "tiktok-oauth")
            .id(UUID.randomUUID().toString())
            .issuedAt(Date.from(now))
            .expiration(Date.from(now.plusSeconds(STATE_TTL_SECONDS)))
            .signWith(signingKey)
            .compact();
    }

    public OAuthState parseState(String state) {
        Claims claims = Jwts.parser()
            .verifyWith(signingKey)
            .build()
            .parseSignedClaims(state)
            .getPayload();
        if (!"tiktok-oauth".equals(claims.get("purpose", String.class))) {
            throw new IllegalArgumentException("Invalid OAuth state purpose");
        }
        return new OAuthState(
            UUID.fromString(claims.getSubject()),
            UUID.fromString(claims.get("channelId", String.class))
        );
    }

    public record OAuthState(UUID userId, UUID channelId) {
    }
}
