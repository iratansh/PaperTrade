package com.papertrade.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.Duration;

/**
 * Finnhub market data service implementation
 *
 * Features:
 * - Redis caching (5s TTL) to reduce API calls
 * - Cache-aside pattern
 * - Reactive WebClient for non-blocking HTTP calls
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FinnhubMarketDataService implements MarketDataService {

    private final WebClient.Builder webClientBuilder;
    private final ReactiveRedisTemplate<String, String> redisTemplate;

    @Value("${finnhub.api-key}")
    private String apiKey;

    @Value("${cache.market-data-ttl:5}")
    private int cacheTtlSeconds;

    private static final String FINNHUB_BASE_URL = "https://finnhub.io/api/v1";
    private static final String CACHE_KEY_PREFIX = "price:";

    /**
     * Get current price with caching
     * Cache-aside pattern:
     * 1. Check cache
     * 2. If miss, fetch from API
     * 3. Store in cache
     */
    @Override
    public Mono<BigDecimal> getCurrentPrice(String symbol) {
        String cacheKey = CACHE_KEY_PREFIX + symbol;

        // Try cache first
        return redisTemplate.opsForValue().get(cacheKey)
            .flatMap(cachedPrice -> {
                log.debug("Cache hit for {}: {}", symbol, cachedPrice);
                return Mono.just(new BigDecimal(cachedPrice));
            })
            .switchIfEmpty(Mono.defer(() -> {
                // Cache miss - fetch from Finnhub API
                log.debug("Cache miss for {}, fetching from Finnhub", symbol);
                return fetchPriceFromApi(symbol)
                    .flatMap(price -> cachePriceAndReturn(cacheKey, price));
            }));
    }

    /**
     * Fetch price from Finnhub API
     */
    private Mono<BigDecimal> fetchPriceFromApi(String symbol) {
        WebClient webClient = webClientBuilder
            .baseUrl(FINNHUB_BASE_URL)
            .build();

        return webClient.get()
            .uri(uriBuilder -> uriBuilder
                .path("/quote")
                .queryParam("symbol", symbol)
                .queryParam("token", apiKey)
                .build())
            .retrieve()
            .bodyToMono(FinnhubQuoteResponse.class)
            .map(response -> {
                if (response.getCurrentPrice() == null) {
                    throw new IllegalArgumentException("Invalid symbol: " + symbol);
                }
                return response.getCurrentPrice();
            })
            .doOnError(error -> log.error("Failed to fetch price for {}: {}", symbol, error.getMessage()));
    }

    /**
     * Cache price and return it
     */
    private Mono<BigDecimal> cachePriceAndReturn(String cacheKey, BigDecimal price) {
        return redisTemplate.opsForValue()
            .set(cacheKey, price.toString(), Duration.ofSeconds(cacheTtlSeconds))
            .thenReturn(price)
            .doOnSuccess(p -> log.debug("Cached price: {} = {}", cacheKey, price));
    }

    @Override
    public Mono<String> searchSymbol(String query) {
        // TODO: Implement symbol search
        return Mono.just("Search not implemented yet");
    }

    /**
     * Finnhub quote response DTO
     */
    @lombok.Data
    private static class FinnhubQuoteResponse {
        private BigDecimal c; // Current price
        private BigDecimal h; // High price of the day
        private BigDecimal l; // Low price of the day
        private BigDecimal o; // Open price
        private BigDecimal pc; // Previous close price

        public BigDecimal getCurrentPrice() {
            return c;
        }
    }
}
