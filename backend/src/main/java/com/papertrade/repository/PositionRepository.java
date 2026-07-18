package com.papertrade.repository;

import com.papertrade.domain.Position;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.UUID;

@Repository
public interface PositionRepository extends R2dbcRepository<Position, UUID> {

    /**
     * Get all positions for an account (user's entire portfolio)
     * Returns multiple positions because user can hold different stocks (AAPL, TSLA, GOOGL, etc.)
     * But only ONE position per stock per account (enforced by UNIQUE constraint)
     *
     * Auto-generated: SELECT * FROM positions WHERE account_id = ?
     */
    Flux<Position> findByAccountId(UUID accountId);

    /**
     * Find THE position for a specific stock in this account
     * Returns Mono (0 or 1 result) because of UNIQUE(account_id, symbol) constraint
     *
     * Use case:
     * - Check if user already owns this stock before buying more
     * - Get current position to update average cost when adding shares
     *
     * Auto-generated: SELECT * FROM positions WHERE account_id = ? AND symbol = ?
     */
    Mono<Position> findByAccountIdAndSymbol(UUID accountId, String symbol);

    /**
     * Get position with PESSIMISTIC LOCK (for concurrent sell operations)
     * Prevents race condition where user tries to sell more shares than owned
     *
     * Example scenario without lock:
     * - Position: 10 shares of AAPL
     * - Request 1: Sell 8 shares (reads 10, calculates 10-8=2)
     * - Request 2: Sell 5 shares (reads 10, calculates 10-5=5)
     * - Both commit → oversold!
     *
     * With lock:
     * - Request 1: LOCKS position, sells 8, updates to 2, UNLOCKS
     * - Request 2: WAITS for lock, reads 2, rejects (insufficient shares)
     */
    @Query("SELECT * FROM positions WHERE account_id = :accountId AND symbol = :symbol FOR UPDATE")
    Mono<Position> findByAccountIdAndSymbolWithLock(@Param("accountId") UUID accountId,
                                                      @Param("symbol") String symbol);

    /**
     * Update current price for a position (called when market data updates)
     * This keeps the position's cached price fresh for P&L calculations
     */
    @Query("UPDATE positions SET current_price = :price, updated_at = CURRENT_TIMESTAMP " +
           "WHERE account_id = :accountId AND symbol = :symbol")
    Mono<Void> updateCurrentPrice(@Param("accountId") UUID accountId,
                                   @Param("symbol") String symbol,
                                   @Param("price") BigDecimal price);

    /**
     * Delete positions with zero quantity (cleanup after selling all shares)
     * When user sells all shares, we remove the position entirely
     */
    @Query("DELETE FROM positions WHERE account_id = :accountId AND quantity = 0")
    Mono<Void> deleteZeroQuantityPositions(@Param("accountId") UUID accountId);
}
