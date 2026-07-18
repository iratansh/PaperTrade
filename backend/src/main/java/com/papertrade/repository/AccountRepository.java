package com.papertrade.repository;

import com.papertrade.domain.TradingAccount;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.UUID;

@Repository
public interface AccountRepository extends R2dbcRepository<TradingAccount, UUID> {

    /**
     * Find account by user ID
     * Auto-generated query: SELECT * FROM accounts WHERE user_id = ?
     */
    Mono<TradingAccount> findByUserId(UUID userId);

    /**
     * Find account with PESSIMISTIC LOCK for critical balance updates
     * The "FOR UPDATE" locks the row until the transaction commits
     *
     * Use this when placing orders to prevent race conditions:
     * - Thread 1: Lock account, check balance, place order
     * - Thread 2: WAITS for lock...
     * - Thread 1: Commits (unlocks)
     * - Thread 2: Gets lock, sees updated balance
     */
    @Query("SELECT * FROM accounts WHERE account_id = :accountId FOR UPDATE")
    Mono<TradingAccount> findByIdWithLock(@Param("accountId") UUID accountId);

    /**
     * Update balance atomically
     * This is an alternative to fetching, modifying, and saving
     */
    @Query("UPDATE accounts SET balance = balance + :amount WHERE account_id = :accountId")
    Mono<Void> updateBalance(@Param("accountId") UUID accountId,
                             @Param("amount") BigDecimal amount);
}
