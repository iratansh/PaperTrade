package com.papertrade.config;

import com.papertrade.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.authentication.HttpStatusServerEntryPoint;

/**
 * Reactive (WebFlux) security configuration: stateless JWT authentication.
 *
 * - /api/auth/** and market-data reads are public.
 * - Everything else requires a valid access token (added by the JWT WebFilter).
 * - Unauthenticated requests to protected endpoints get 401 (no login redirect).
 */
@Configuration
@EnableWebFluxSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        return http
            .csrf(ServerHttpSecurity.CsrfSpec::disable)
            .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
            .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
            .authorizeExchange(exchanges -> exchanges
                .pathMatchers("/api/auth/**").permitAll()
                // Market data (quotes/search/history) and the live SSE price feed
                // are public (EventSource can't send an auth header).
                .pathMatchers("/api/stocks/**").permitAll()
                .pathMatchers("/api/stream/**").permitAll()
                .anyExchange().authenticated())
            .exceptionHandling(ex -> ex.authenticationEntryPoint(
                new HttpStatusServerEntryPoint(HttpStatus.UNAUTHORIZED)))
            .addFilterAt(jwtAuthenticationFilter, SecurityWebFiltersOrder.AUTHENTICATION)
            .build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
