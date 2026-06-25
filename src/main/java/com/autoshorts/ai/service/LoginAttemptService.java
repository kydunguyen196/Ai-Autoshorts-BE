package com.autoshorts.ai.service;

import com.autoshorts.ai.config.SecurityProperties;
import com.autoshorts.ai.entity.AppUser;
import com.autoshorts.ai.repository.AppUserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

/**
 * Tracks failed-login attempts and account lockout. Each mutation runs in its OWN transaction
 * (REQUIRES_NEW) so the counter is persisted even when the surrounding login flow throws on a
 * bad password and would otherwise roll back.
 */
@Service
public class LoginAttemptService {

    private static final Logger log = LoggerFactory.getLogger(LoginAttemptService.class);

    private final AppUserRepository appUserRepository;
    private final SecurityProperties securityProperties;

    public LoginAttemptService(AppUserRepository appUserRepository, SecurityProperties securityProperties) {
        this.appUserRepository = appUserRepository;
        this.securityProperties = securityProperties;
    }

    /** Returns true if the account is currently locked. */
    public boolean isLocked(AppUser user, Instant now) {
        return user.getLockoutUntil() != null && user.getLockoutUntil().isAfter(now);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordFailure(UUID userId) {
        appUserRepository.findById(userId).ifPresent(user -> {
            int attempts = user.getFailedLoginAttempts() + 1;
            user.setFailedLoginAttempts(attempts);
            if (attempts >= securityProperties.getLockoutThreshold()) {
                user.setLockoutUntil(Instant.now().plusSeconds(securityProperties.getLockoutMinutes() * 60));
                user.setFailedLoginAttempts(0);
                log.warn("event=account_locked userId={} lockoutMinutes={}", userId, securityProperties.getLockoutMinutes());
            }
            appUserRepository.save(user);
        });
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void reset(UUID userId) {
        appUserRepository.findById(userId).ifPresent(user -> {
            if (user.getFailedLoginAttempts() != 0 || user.getLockoutUntil() != null) {
                user.setFailedLoginAttempts(0);
                user.setLockoutUntil(null);
                appUserRepository.save(user);
            }
        });
    }
}
