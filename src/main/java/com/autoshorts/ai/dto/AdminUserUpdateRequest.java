package com.autoshorts.ai.dto;

import com.autoshorts.ai.entity.UserRole;

public class AdminUserUpdateRequest {

    private UserRole role;
    private Boolean enabled;

    public UserRole getRole() {
        return role;
    }

    public void setRole(UserRole role) {
        this.role = role;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }
}
