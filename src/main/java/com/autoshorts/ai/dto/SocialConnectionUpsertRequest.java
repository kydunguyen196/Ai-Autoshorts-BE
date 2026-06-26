package com.autoshorts.ai.dto;

import com.autoshorts.ai.entity.SocialConnectionStatus;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Manual connection upsert for a social platform (YouTube / Instagram). Mirrors
 * {@link TikTokConnectionUpsertRequest}; the platform comes from the URL path.
 */
public class SocialConnectionUpsertRequest {

    private UUID channelId;

    @Size(max = 255, message = "platformAccountId must be <= 255 characters")
    private String platformAccountId;

    @Size(max = 255, message = "platformUsername must be <= 255 characters")
    private String platformUsername;

    @Size(max = 4096, message = "accessToken must be <= 4096 characters")
    private String accessToken;

    @Size(max = 4096, message = "refreshToken must be <= 4096 characters")
    private String refreshToken;

    private Instant tokenExpiresAt;

    private List<String> scopes;

    private SocialConnectionStatus status = SocialConnectionStatus.ACTIVE;

    public UUID getChannelId() {
        return channelId;
    }

    public void setChannelId(UUID channelId) {
        this.channelId = channelId;
    }

    public String getPlatformAccountId() {
        return platformAccountId;
    }

    public void setPlatformAccountId(String platformAccountId) {
        this.platformAccountId = platformAccountId;
    }

    public String getPlatformUsername() {
        return platformUsername;
    }

    public void setPlatformUsername(String platformUsername) {
        this.platformUsername = platformUsername;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    public Instant getTokenExpiresAt() {
        return tokenExpiresAt;
    }

    public void setTokenExpiresAt(Instant tokenExpiresAt) {
        this.tokenExpiresAt = tokenExpiresAt;
    }

    public List<String> getScopes() {
        return scopes;
    }

    public void setScopes(List<String> scopes) {
        this.scopes = scopes;
    }

    public SocialConnectionStatus getStatus() {
        return status;
    }

    public void setStatus(SocialConnectionStatus status) {
        this.status = status;
    }
}
