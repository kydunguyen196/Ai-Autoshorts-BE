package com.autoshorts.ai.service;

import com.autoshorts.ai.exception.ResourceNotFoundException;
import com.autoshorts.ai.repository.AppUserRepository;
import com.autoshorts.ai.security.AppUserPrincipal;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class CurrentUserService {

    private final AppUserRepository appUserRepository;

    public CurrentUserService(AppUserRepository appUserRepository) {
        this.appUserRepository = appUserRepository;
    }

    public UUID requireCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof AppUserPrincipal principal)) {
            throw new ResourceNotFoundException("Authenticated user context not found");
        }
        return principal.getUserId();
    }

    public com.autoshorts.ai.entity.AppUser requireCurrentUserEntity() {
        UUID userId = requireCurrentUserId();
        return appUserRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));
    }
}
