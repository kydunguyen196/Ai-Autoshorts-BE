package com.autoshorts.ai.client.model;

import java.time.Instant;
import java.util.List;

/**
 * Normalized result of a TikTok OAuth token exchange or refresh.
 */
public record TikTokTokenResponse(
    String accessToken,
    String refreshToken,
    String openId,
    List<String> scopes,
    Instant accessTokenExpiresAt,
    Instant refreshTokenExpiresAt
) {
}
