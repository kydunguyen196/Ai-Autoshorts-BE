package com.autoshorts.ai.service;

import com.autoshorts.ai.dto.AuthResponse;
import com.autoshorts.ai.dto.CurrentUserResponse;
import com.autoshorts.ai.dto.LoginRequest;
import com.autoshorts.ai.dto.RegisterRequest;
import com.autoshorts.ai.entity.AppUser;
import com.autoshorts.ai.entity.UserRole;
import com.autoshorts.ai.exception.BadRequestException;
import com.autoshorts.ai.repository.AppUserRepository;
import com.autoshorts.ai.security.AppUserPrincipal;
import com.autoshorts.ai.security.JwtTokenService;
import com.autoshorts.ai.util.UserMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final AppUserRepository appUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenService jwtTokenService;
    private final ChannelService channelService;
    private final CurrentUserService currentUserService;

    public AuthService(
        AppUserRepository appUserRepository,
        PasswordEncoder passwordEncoder,
        AuthenticationManager authenticationManager,
        JwtTokenService jwtTokenService,
        ChannelService channelService,
        CurrentUserService currentUserService
    ) {
        this.appUserRepository = appUserRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtTokenService = jwtTokenService;
        this.channelService = channelService;
        this.currentUserService = currentUserService;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String email = normalizeEmail(request.getEmail());
        if (appUserRepository.existsByEmailIgnoreCase(email)) {
            throw new BadRequestException("Email is already registered");
        }

        AppUser user = new AppUser();
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setDisplayName(request.getDisplayName().trim());
        user.setRole(UserRole.USER);
        AppUser saved;
        try {
            saved = appUserRepository.save(user);
        } catch (DataIntegrityViolationException ex) {
            throw new BadRequestException("Email is already registered");
        }

        channelService.ensureDefaultChannel(saved.getId());
        AppUserPrincipal principal = new AppUserPrincipal(saved);
        String token = jwtTokenService.generateToken(principal);

        log.info("event=user_registered userId={} email={}", saved.getId(), saved.getEmail());
        return buildAuthResponse(saved, token);
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        String email = normalizeEmail(request.getEmail());
        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(email, request.getPassword())
            );
        } catch (AuthenticationException ex) {
            throw new BadRequestException("Invalid email or password");
        }

        AppUserPrincipal principal = (AppUserPrincipal) authentication.getPrincipal();
        AppUser user = appUserRepository.findById(principal.getUserId())
            .orElseThrow(() -> new BadRequestException("User account not found"));
        String token = jwtTokenService.generateToken(principal);

        log.info("event=user_logged_in userId={} email={}", user.getId(), user.getEmail());
        return buildAuthResponse(user, token);
    }

    @Transactional
    public CurrentUserResponse getCurrentUser() {
        AppUser user = currentUserService.requireCurrentUserEntity();
        CurrentUserResponse response = new CurrentUserResponse();
        response.setUser(UserMapper.toResponse(user));
        response.setDefaultChannel(channelService.getDefaultChannelResponse(user.getId()));
        return response;
    }

    private AuthResponse buildAuthResponse(AppUser user, String token) {
        AuthResponse response = new AuthResponse();
        response.setAccessToken(token);
        response.setExpiresInSeconds(jwtTokenService.getTokenTtlSeconds());
        response.setUser(UserMapper.toResponse(user));
        response.setDefaultChannel(channelService.getDefaultChannelResponse(user.getId()));
        return response;
    }

    private String normalizeEmail(String rawEmail) {
        return rawEmail.trim().toLowerCase(Locale.ROOT);
    }
}
