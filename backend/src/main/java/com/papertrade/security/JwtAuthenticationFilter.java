package com.papertrade.security;

import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * Reactive JWT authentication filter.
 *
 * Validates the "Authorization: Bearer <token>" header and, on a valid access
 * token, writes an Authentication into the reactive security context. Because
 * the context is carried in the Reactor Context (not a thread-local), it
 * propagates correctly across the threads a reactive request hops between -
 * which is exactly what the servlet-based filter could not do.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter implements WebFilter {

    private final JwtService jwtService;

    private static final String BEARER_PREFIX = "Bearer ";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);

        if (authHeader != null && authHeader.startsWith(BEARER_PREFIX)) {
            String token = authHeader.substring(BEARER_PREFIX.length());
            try {
                Claims claims = jwtService.parse(token);
                if (jwtService.isAccessToken(claims)) {
                    var authentication = new UsernamePasswordAuthenticationToken(
                        claims.getSubject(), null, List.of());
                    return chain.filter(exchange)
                        .contextWrite(ReactiveSecurityContextHolder.withAuthentication(authentication));
                }
            } catch (Exception e) {
                // Invalid token: proceed unauthenticated (protected routes -> 401)
                log.debug("Rejected JWT: {}", e.getMessage());
            }
        }

        return chain.filter(exchange);
    }
}
