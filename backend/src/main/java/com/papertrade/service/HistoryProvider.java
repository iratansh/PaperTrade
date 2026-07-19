package com.papertrade.service;

import com.papertrade.domain.enums.HistoryRange;
import com.papertrade.dto.Candle;
import reactor.core.publisher.Flux;

/**
 * Provider of historical OHLC price data for charting.
 *
 * Separate from {@link MarketDataService} (real-time quotes/search) by design:
 * historical candles come from Twelve Data, while live quotes and streaming
 * come from Finnhub. Each provider is used for what its free tier does best.
 */
public interface HistoryProvider {

    /**
     * Get historical candles for a symbol over the given range,
     * ordered chronologically (oldest first).
     */
    Flux<Candle> getHistory(String symbol, HistoryRange range);
}
