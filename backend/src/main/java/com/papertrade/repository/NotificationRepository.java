package com.papertrade.repository;

import com.papertrade.domain.Notification;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Repository
public interface NotificationRepository extends R2dbcRepository<Notification, UUID> {

    Flux<Notification> findByUserIdOrderByCreatedAtDesc(UUID userId);

    Mono<Long> countByUserIdAndIsReadFalse(UUID userId);

    @Query("UPDATE notifications SET is_read = true WHERE user_id = :userId AND is_read = false")
    Mono<Void> markAllRead(@Param("userId") UUID userId);
}
