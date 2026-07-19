package com.papertrade.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;

/**
 * Issues and validates JWTs.
 *
 * The token subject is the user's UUID, so downstream code can identify the
 * caller without another DB lookup. Two token types are issued: a short-lived
 * access token (sent on every request) and a longer-lived refresh token
 * (exchanged for a new access token via /api/auth/refresh).
 */
@Component
public class JwtService {

    private final SecretKey key;
    private final long accessExpirationMs;
    private final long refreshExpirationMs;

    private static final String CLAIM_TYPE = "type";
    private static final String TYPE_ACCESS = "access";
    private static final String TYPE_REFRESH = "refresh";

    public JwtService(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.expiration}") long accessExpirationMs,
            @Value("${jwt.refresh-expiration}") long refreshExpirationMs) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessExpirationMs = accessExpirationMs;
        this.refreshExpirationMs = refreshExpirationMs;
    }

    public String generateAccessToken(UUID userId, String username) {
        return buildToken(userId, username, TYPE_ACCESS, accessExpirationMs);
    }

    public String generateRefreshToken(UUID userId, String username) {
        return buildToken(userId, username, TYPE_REFRESH, refreshExpirationMs);
    }

    private String buildToken(UUID userId, String username, String type, long ttlMs) {
        Date now = new Date();
        return Jwts.builder()
            .subject(userId.toString())
            .claim("username", username)
            .claim(CLAIM_TYPE, type)
            .issuedAt(now)
            .expiration(new Date(now.getTime() + ttlMs))
            .signWith(key)
            .compact();
    }

    /**
     * Parse and validate a token, returning its claims.
     * Throws JwtException (invalid signature, expired, malformed) on failure.
     */
    public Claims parse(String token) {
        return Jwts.parser()
            .verifyWith(key)
            .build()
            .parseSignedClaims(token)
            .getPayload();
    }

    public UUID extractUserId(String token) {
        return UUID.fromString(parse(token).getSubject());
    }

    public boolean isRefreshToken(Claims claims) {
        return TYPE_REFRESH.equals(claims.get(CLAIM_TYPE, String.class));
    }

    public boolean isAccessToken(Claims claims) {
        return TYPE_ACCESS.equals(claims.get(CLAIM_TYPE, String.class));
    }
}
