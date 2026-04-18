package com.autoshorts.ai.util;

import com.autoshorts.ai.dto.ChannelResponse;
import com.autoshorts.ai.entity.Channel;

public final class ChannelMapper {

    private ChannelMapper() {
    }

    public static ChannelResponse toResponse(Channel channel) {
        ChannelResponse response = new ChannelResponse();
        response.setId(channel.getId());
        response.setName(channel.getName());
        response.setDescription(channel.getDescription());
        response.setIsDefault(channel.getIsDefault());
        response.setCreatedAt(channel.getCreatedAt());
        response.setUpdatedAt(channel.getUpdatedAt());
        return response;
    }
}
