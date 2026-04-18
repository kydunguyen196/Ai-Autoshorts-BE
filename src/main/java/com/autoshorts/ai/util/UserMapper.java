package com.autoshorts.ai.util;

import com.autoshorts.ai.dto.UserProfileResponse;
import com.autoshorts.ai.entity.AppUser;

public final class UserMapper {

    private UserMapper() {
    }

    public static UserProfileResponse toResponse(AppUser user) {
        UserProfileResponse response = new UserProfileResponse();
        response.setId(user.getId());
        response.setEmail(user.getEmail());
        response.setDisplayName(user.getDisplayName());
        response.setRole(user.getRole());
        response.setCreatedAt(user.getCreatedAt());
        response.setUpdatedAt(user.getUpdatedAt());
        return response;
    }
}
