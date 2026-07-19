package com.papertrade.service;

import com.papertrade.dto.SymbolMatch;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;

/**
 * Real-time market data service interface (quotes + symbol search).
 * Historical candles live in {@link HistoryProvider}.
 *
 * This demonstrates:
 * - Dependency Inversion Principle (depend on abstraction, not concrete implementation)
 * - Strategy Pattern (different market data providers)
 */
public interface MarketDataService {

    /**
     * Get current price for a symbol
     * @param symbol Stock ticker (e.g., "AAPL")
     * @return Current price
     */
    Mono<BigDecimal> getCurrentPrice(String symbol);

    /**
     * Search for stocks by query
     * @param query Search term (ticker or company name)
     * @return Matching symbols
     */
    Flux<SymbolMatch> searchSymbol(String query);
}
