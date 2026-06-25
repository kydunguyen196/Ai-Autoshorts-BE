package com.autoshorts.ai.config;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.security")
public class SecurityProperties {

    @NotBlank
    private String jwtSecret;

    @Min(300)
    private long tokenTtlSeconds = 900;

    /** Lifetime of a rotating refresh token (default 30 days). */
    @Min(3600)
    private long refreshTokenTtlSeconds = 2_592_000;

    /** Failed-login attempts before an account is temporarily locked. */
    @Min(1)
    private int lockoutThreshold = 5;

    /** How long an account stays locked after hitting the threshold (minutes). */
    @Min(1)
    private long lockoutMinutes = 15;

    /**
     * Base64-encoded AES key (16/24/32 bytes) used to encrypt third-party OAuth tokens at rest.
     * When blank, a key is derived from {@link #jwtSecret} (dev convenience only).
     */
    private String tokenEncryptionKey;

    public String getJwtSecret() {
        return jwtSecret;
    }

    public void setJwtSecret(String jwtSecret) {
        this.jwtSecret = jwtSecret;
    }

    public long getTokenTtlSeconds() {
        return tokenTtlSeconds;
    }

    public void setTokenTtlSeconds(long tokenTtlSeconds) {
        this.tokenTtlSeconds = tokenTtlSeconds;
    }

    public long getRefreshTokenTtlSeconds() {
        return refreshTokenTtlSeconds;
    }

    public void setRefreshTokenTtlSeconds(long refreshTokenTtlSeconds) {
        this.refreshTokenTtlSeconds = refreshTokenTtlSeconds;
    }

    public int getLockoutThreshold() {
        return lockoutThreshold;
    }

    public void setLockoutThreshold(int lockoutThreshold) {
        this.lockoutThreshold = lockoutThreshold;
    }

    public long getLockoutMinutes() {
        return lockoutMinutes;
    }

    public void setLockoutMinutes(long lockoutMinutes) {
        this.lockoutMinutes = lockoutMinutes;
    }

    public String getTokenEncryptionKey() {
        return tokenEncryptionKey;
    }

    public void setTokenEncryptionKey(String tokenEncryptionKey) {
        this.tokenEncryptionKey = tokenEncryptionKey;
    }
}
