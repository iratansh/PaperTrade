package com.papertrade.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.papertrade.domain.enums.HistoryRange;
import com.papertrade.dto.Candle;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Twelve Data implementation of {@link HistoryProvider}.
 *
 * Fetches historical OHLC candles from the Twelve Data {@code /time_series}
 * endpoint and caches them in Redis. Past candles are immutable, so caching
 * keeps us well under the free tier's 800 credits/day limit.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TwelveDataHistoryService implements HistoryProvider {

    private final WebClient.Builder webClientBuilder;
    private final ReactiveRedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    @Value("${twelvedata.api-key}")
    private String apiKey;

    private static final String BASE_URL = "https://api.twelvedata.com";
    private static final String CACHE_PREFIX = "history:";

    @Override
    public Flux<Candle> getHistory(String symbol, HistoryRange range) {
        String cacheKey = CACHE_PREFIX + symbol + ":" + range.name();

        // Cache-aside: try Redis first, fall back to the API on a miss
        return redisTemplate.opsForValue().get(cacheKey)
            .flatMapMany(cached -> {
                log.debug("Cache hit for history {} {}", symbol, range);
                return Flux.fromIterable(deserialize(cached));
            })
            .switchIfEmpty(fetchFromApi(symbol, range, cacheKey));
    }

    private Flux<Candle> fetchFromApi(String symbol, HistoryRange range, String cacheKey) {
        log.debug("Cache miss for history {} {}, fetching from Twelve Data", symbol, range);
        LocalDate today = LocalDate.now();
        WebClient webClient = webClientBuilder.baseUrl(BASE_URL).build();

        return webClient.get()
            .uri(uriBuilder -> {
                uriBuilder
                    .path("/time_series")
                    .queryParam("symbol", symbol)
                    .queryParam("interval", range.interval())
                    .queryParam("order", "ASC") // oldest first, ready for charting
                    .queryParam("apikey", apiKey);
                // Intraday ranges query by recent-bar count (weekend-safe);
                // daily ranges query from a start date.
                if (range.usesOutputSize()) {
                    uriBuilder.queryParam("outputsize", range.outputSize());
                } else {
                    uriBuilder.queryParam("start_date", range.startDate(today).toString());
                }
                return uriBuilder.build();
            })
            // exchangeToMono reads the body regardless of HTTP status, so Twelve
            // Data's error JSON (which comes back as HTTP 400) is parsed and
            // surfaced as a clean 400 instead of throwing -> a generic 500.
            .exchangeToMono(response -> response.bodyToMono(TwelveDataResponse.class))
            .flatMapMany(response -> {
                if (!"ok".equalsIgnoreCase(response.getStatus()) || response.getValues() == null) {
                    return Flux.error(new IllegalArgumentException(
                        "No history for " + symbol + ": "
                            + (response.getMessage() != null ? response.getMessage() : "unknown error")));
                }

                List<Candle> candles = response.getValues().stream()
                    .map(v -> new Candle(v.getDatetime(), v.getOpen(), v.getHigh(),
                                         v.getLow(), v.getClose(), v.getVolume()))
                    .toList();

                // Best-effort cache write, then emit the candles
                return cacheCandles(cacheKey, candles, range)
                    .thenMany(Flux.fromIterable(candles));
            })
            .doOnError(error -> log.error("Failed to fetch history for {} {}: {}",
                                          symbol, range, error.getMessage()));
    }

    private Mono<Void> cacheCandles(String cacheKey, List<Candle> candles, HistoryRange range) {
        try {
            String json = objectMapper.writeValueAsString(candles);
            return redisTemplate.opsForValue()
                .set(cacheKey, json, range.cacheTtl())
                .then();
        } catch (Exception e) {
            // Caching is best-effort; never fail the request because of it
            log.warn("Failed to cache history for {}: {}", cacheKey, e.getMessage());
            return Mono.empty();
        }
    }

    private List<Candle> deserialize(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<List<Candle>>() {});
        } catch (Exception e) {
            log.warn("Failed to deserialize cached history: {}", e.getMessage());
            return List.of();
        }
    }

    /**
     * Twelve Data /time_series response shape.
     * Numeric fields arrive as JSON strings; Jackson maps them to BigDecimal.
     */
    @Data
    static class TwelveDataResponse {
        private String status;
        private String message;
        private String code;
        private List<TwelveDataValue> values;
    }

    @Data
    static class TwelveDataValue {
        private String datetime;
        private BigDecimal open;
        private BigDecimal high;
        private BigDecimal low;
        private BigDecimal close;
        private BigDecimal volume;
    }
}
