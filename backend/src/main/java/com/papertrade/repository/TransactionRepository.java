package com.papertrade.repository;

import com.papertrade.domain.Transaction;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

import java.util.UUID;

@Repository
public interface TransactionRepository extends R2dbcRepository<Transaction, UUID> {

    /**
     * Get all transactions for an account (audit log)
     * Auto-generated: SELECT * FROM transactions WHERE account_id = ?
     * ORDER BY timestamp DESC
     */
    Flux<Transaction> findByAccountIdOrderByTimestampDesc(UUID accountId);

    /**
     * Get transactions for a specific order
     * Auto-generated: SELECT * FROM transactions WHERE order_id = ?
     */
    Flux<Transaction> findByOrderId(UUID orderId);

    /**
     * Get transactions for a specific symbol
     * Useful for calculating realized P&L on a per-stock basis
     */
    Flux<Transaction> findByAccountIdAndSymbol(UUID accountId, String symbol);
}
