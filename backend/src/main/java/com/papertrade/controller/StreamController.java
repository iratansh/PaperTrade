package com.papertrade.controller;

import com.papertrade.dto.PriceUpdate;
import com.papertrade.service.FinnhubStreamService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Server-Sent Events endpoint for live prices.
 *
 * SSE (not raw WebSocket) because the browser's EventSource is one-directional -
 * exactly what price ticks need - and it composes cleanly with WebFlux as a
 * Flux<ServerSentEvent>. Kept public so EventSource (which can't send an auth
 * header) works; prices are public market data anyway.
 */
@Slf4j
@RestController
@RequestMapping("/api/stream")
@RequiredArgsConstructor
public class StreamController {

    private final FinnhubStreamService streamService;

    @GetMapping(value = "/prices", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<PriceUpdate>> streamPrices(@RequestParam String symbols) {
        Set<String> symbolSet = Arrays.stream(symbols.split(","))
            .map(String::trim)
            .filter(s -> !s.isEmpty())
            .map(String::toUpperCase)
            .collect(Collectors.toSet());

        Flux<ServerSentEvent<PriceUpdate>> prices = streamService.stream(symbolSet)
            .map(update -> ServerSentEvent.builder(update).event("price").build());

        // Periodic comment/heartbeat keeps intermediaries from closing an idle stream
        Flux<ServerSentEvent<PriceUpdate>> heartbeat = Flux.interval(Duration.ofSeconds(25))
            .map(i -> ServerSentEvent.<PriceUpdate>builder().comment("keep-alive").build());

        return Flux.merge(prices, heartbeat);
    }
}
