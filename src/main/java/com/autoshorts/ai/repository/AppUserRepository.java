package com.autoshorts.ai.repository;

import com.autoshorts.ai.entity.AppUser;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface AppUserRepository extends JpaRepository<AppUser, UUID> {

    Optional<AppUser> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    Page<AppUser> findAllByEmailContainingIgnoreCaseOrDisplayNameContainingIgnoreCase(
        String email,
        String displayName,
        Pageable pageable
    );
}
