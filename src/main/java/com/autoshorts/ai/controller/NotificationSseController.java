package com.autoshorts.ai.controller;

import com.autoshorts.ai.exception.BadRequestException;
import com.autoshorts.ai.security.JwtTokenService;
import com.autoshorts.ai.service.NotificationSseService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.UUID;

/**
 * Real-time notification stream. EventSource cannot set an Authorization header, so the
 * JWT is passed as a {@code token} query parameter and validated here directly. This
 * endpoint is permitted in SecurityConfig precisely because it performs its own auth.
 */
@RestController
@RequestMapping("/api/notifications")
public class NotificationSseController {

    private final JwtTokenService jwtTokenService;
    private final NotificationSseService sseService;

    public NotificationSseController(JwtTokenService jwtTokenService, NotificationSseService sseService) {
        this.jwtTokenService = jwtTokenService;
        this.sseService = sseService;
    }

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(@RequestParam("token") String token) {
        UUID userId;
        try {
            userId = jwtTokenService.parseUserId(token);
        } catch (Exception ex) {
            throw new BadRequestException("Invalid or expired token");
        }
        return sseService.register(userId);
    }
}
