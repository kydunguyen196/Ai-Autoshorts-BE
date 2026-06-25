package com.autoshorts.ai.dto;

public class AuthResponse {

    private String tokenType = "Bearer";
    private String accessToken;
    private String refreshToken;
    private long expiresInSeconds;
    private UserProfileResponse user;
    private ChannelResponse defaultChannel;

    public String getTokenType() {
        return tokenType;
    }

    public void setTokenType(String tokenType) {
        this.tokenType = tokenType;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public long getExpiresInSeconds() {
        return expiresInSeconds;
    }

    public void setExpiresInSeconds(long expiresInSeconds) {
        this.expiresInSeconds = expiresInSeconds;
    }

    public UserProfileResponse getUser() {
        return user;
    }

    public void setUser(UserProfileResponse user) {
        this.user = user;
    }

    public ChannelResponse getDefaultChannel() {
        return defaultChannel;
    }

    public void setDefaultChannel(ChannelResponse defaultChannel) {
        this.defaultChannel = defaultChannel;
    }
}
