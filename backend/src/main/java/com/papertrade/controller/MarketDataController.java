package com.papertrade.controller;

import com.papertrade.domain.enums.HistoryRange;
import com.papertrade.dto.Candle;
import com.papertrade.service.HistoryProvider;
import com.papertrade.service.MarketDataService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;

/**
 * REST controller for market data operations
 *
 * Endpoints:
 * - GET /api/stocks/{symbol}/quote - Get current price
 * - GET /api/stocks/{symbol}/history - Get historical candles for charting
 * - GET /api/stocks/search - Search for stocks
 */
@Slf4j
@RestController
@RequestMapping("/api/stocks")
@RequiredArgsConstructor
public class MarketDataController {

    private final MarketDataService marketDataService;
    private final HistoryProvider historyProvider;

    /**
     * Get current price for a stock
     *
     * GET /api/stocks/AAPL/quote
     */
    @GetMapping("/{symbol}/quote")
    public Mono<QuoteResponse> getQuote(@PathVariable String symbol) {
        log.info("Fetching quote for: {}", symbol);
        return marketDataService.getCurrentPrice(symbol.toUpperCase())
            .map(price -> new QuoteResponse(symbol.toUpperCase(), price));
    }

    /**
     * Get historical price candles for charting.
     *
     * GET /api/stocks/AAPL/history?range=3M
     * range: 1D, 1W, 3M (default), 1Y, YTD
     */
    @GetMapping("/{symbol}/history")
    public Flux<Candle> getHistory(@PathVariable String symbol,
                                   @RequestParam(defaultValue = "3M") String range) {
        log.info("Fetching {} history for: {}", range, symbol);
        return historyProvider.getHistory(symbol.toUpperCase(), HistoryRange.fromCode(range));
    }

    /**
     * Search for stocks
     *
     * GET /api/stocks/search?q=apple
     */
    @GetMapping("/search")
    public Mono<String> searchStocks(@RequestParam String q) {
        log.info("Searching stocks with query: {}", q);
        return marketDataService.searchSymbol(q);
    }

    /**
     * Quote response DTO
     */
    public record QuoteResponse(String symbol, BigDecimal price) {}
}
