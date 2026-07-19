package com.papertrade.controller;

import com.papertrade.service.MarketDataService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;

/**
 * REST controller for market data operations
 *
 * Endpoints:
 * - GET /api/stocks/{symbol}/quote - Get current price
 * - GET /api/stocks/search - Search for stocks
 */
@Slf4j
@RestController
@RequestMapping("/api/stocks")
@RequiredArgsConstructor
public class MarketDataController {

    private final MarketDataService marketDataService;

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
