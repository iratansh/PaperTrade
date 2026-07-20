package com.papertrade.repository;

import com.papertrade.domain.WatchlistItem;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Repository
public interface WatchlistRepository extends R2dbcRepository<WatchlistItem, UUID> {

    Flux<WatchlistItem> findByUserIdOrderByCreatedAtDesc(UUID userId);

    Mono<WatchlistItem> findByUserIdAndSymbol(UUID userId, String symbol);

    Mono<Void> deleteByUserIdAndSymbol(UUID userId, String symbol);

    /**
     * Active (untriggered) alerts for a symbol - checked when a price arrives.
     */
    Flux<WatchlistItem> findBySymbolAndTriggeredFalseAndAlertPriceIsNotNull(String symbol);
}
