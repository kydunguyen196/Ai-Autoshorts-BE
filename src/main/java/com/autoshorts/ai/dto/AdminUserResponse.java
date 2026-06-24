package com.autoshorts.ai.dto;

import com.autoshorts.ai.entity.AppUser;

import java.time.Instant;
import java.util.UUID;

public class AdminUserResponse {

    private UUID id;
    private String email;
    private String displayName;
    private String role;
    private boolean enabled;
    private Instant createdAt;
    private Instant updatedAt;

    public static AdminUserResponse from(AppUser u) {
        AdminUserResponse r = new AdminUserResponse();
        r.id = u.getId();
        r.email = u.getEmail();
        r.displayName = u.getDisplayName();
        r.role = u.getRole() != null ? u.getRole().name() : null;
        r.enabled = u.isEnabled();
        r.createdAt = u.getCreatedAt();
        r.updatedAt = u.getUpdatedAt();
        return r;
    }

    public UUID getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getRole() {
        return role;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
