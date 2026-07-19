package com.papertrade.controller;

import com.papertrade.dto.AuthResponse;
import com.papertrade.dto.LoginRequest;
import com.papertrade.dto.RefreshRequest;
import com.papertrade.dto.RegisterRequest;
import com.papertrade.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

/**
 * Authentication endpoints (public).
 *
 * - POST /api/auth/register - create account + provision $100k portfolio
 * - POST /api/auth/login    - authenticate, receive tokens
 * - POST /api/auth/refresh  - exchange a refresh token for new tokens
 */
@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        log.info("Registering new user: {}", request.getUsername());
        return authService.register(request);
    }

    @PostMapping("/login")
    public Mono<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        log.info("Login attempt for: {}", request.getUsername());
        return authService.login(request);
    }

    @PostMapping("/refresh")
    public Mono<AuthResponse> refresh(@Valid @RequestBody RefreshRequest request) {
        return authService.refresh(request.getRefreshToken());
    }
}
