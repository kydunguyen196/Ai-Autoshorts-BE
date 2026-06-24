package com.autoshorts.ai.config;

import com.autoshorts.ai.entity.AppUser;
import com.autoshorts.ai.entity.UserRole;
import com.autoshorts.ai.repository.AppUserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

/**
 * Promotes the emails configured via {@code app.admin.seed-emails} to {@link UserRole#ADMIN}
 * on startup. Idempotent: only updates users that exist and are not already admins.
 */
@Component
public class AdminSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminSeeder.class);

    private final AppProperties appProperties;
    private final AppUserRepository appUserRepository;

    public AdminSeeder(AppProperties appProperties, AppUserRepository appUserRepository) {
        this.appProperties = appProperties;
        this.appUserRepository = appUserRepository;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        var seedEmails = appProperties.getAdmin().getSeedEmails();
        if (seedEmails == null || seedEmails.isEmpty()) {
            return;
        }
        for (String rawEmail : seedEmails) {
            if (rawEmail == null || rawEmail.isBlank()) {
                continue;
            }
            String email = rawEmail.trim().toLowerCase(Locale.ROOT);
            appUserRepository.findByEmailIgnoreCase(email).ifPresentOrElse(user -> {
                if (user.getRole() != UserRole.ADMIN) {
                    user.setRole(UserRole.ADMIN);
                    appUserRepository.save(user);
                    log.info("event=admin_seeded userId={} email={}", user.getId(), email);
                }
            }, () -> log.warn("event=admin_seed_skipped reason=user_not_found email={}", email));
        }
    }
}
