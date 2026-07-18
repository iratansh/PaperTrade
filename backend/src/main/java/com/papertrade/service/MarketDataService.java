package com.papertrade.service;

import reactor.core.publisher.Mono;

import java.math.BigDecimal;

/**
 * Market data service interface
 * Implementations: FinnhubMarketDataService, AlphaVantageMarketDataService, etc.
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
     * @param query Search term
     * @return List of matching symbols
     */
    Mono<String> searchSymbol(String query);
}
