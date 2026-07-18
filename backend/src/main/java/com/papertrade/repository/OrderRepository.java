package com.papertrade.repository;

import com.papertrade.domain.Order;
import com.papertrade.domain.enums.OrderStatus;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Repository
public interface OrderRepository extends R2dbcRepository<Order, UUID> {

    /**
     * Get all orders for an account (order history)
     * Auto-generated: SELECT * FROM orders WHERE account_id = ?
     * ORDER BY created_at DESC
     */
    Flux<Order> findByAccountIdOrderByCreatedAtDesc(UUID accountId);

    /**
     * Get orders by status (e.g., all pending orders)
     * Auto-generated: SELECT * FROM orders WHERE account_id = ? AND status = ?
     */
    Flux<Order> findByAccountIdAndStatus(UUID accountId, OrderStatus status);

    /**
     * Find all pending limit orders (for background processing)
     * These need to be checked against market prices periodically
     */
    Flux<Order> findByStatus(OrderStatus status);

    /**
     * Find order by idempotency key (prevent duplicate submissions)
     * Auto-generated: SELECT * FROM orders WHERE idempotency_key = ?
     */
    Mono<Order> findByIdempotencyKey(String idempotencyKey);

    /**
     * Check if idempotency key exists
     * Used to quickly reject duplicate order submissions
     */
    Mono<Boolean> existsByIdempotencyKey(String idempotencyKey);
}
