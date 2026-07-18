package com.papertrade.repository;

import com.papertrade.domain.User;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Repository
public interface UserRepository extends R2dbcRepository<User, UUID> {

    /**
     * Find user by username (for login)
     * Auto-generated: SELECT * FROM users WHERE username = ?
     */
    Mono<User> findByUsername(String username);

    /**
     * Find user by email
     * Auto-generated: SELECT * FROM users WHERE email = ?
     */
    Mono<User> findByEmail(String email);

    /**
     * Check if username exists (for registration validation)
     * Auto-generated: SELECT COUNT(*) > 0 FROM users WHERE username = ?
     */
    Mono<Boolean> existsByUsername(String username);

    /**
     * Check if email exists
     * Auto-generated: SELECT COUNT(*) > 0 FROM users WHERE email = ?
     */
    Mono<Boolean> existsByEmail(String email);
}
