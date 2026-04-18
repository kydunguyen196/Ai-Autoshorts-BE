package com.autoshorts.ai.dto;

public class CurrentUserResponse {

    private UserProfileResponse user;
    private ChannelResponse defaultChannel;

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
