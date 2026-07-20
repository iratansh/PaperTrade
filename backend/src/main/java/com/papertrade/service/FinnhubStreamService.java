package com.papertrade.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.papertrade.dto.PriceUpdate;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.client.ReactorNettyWebSocketClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import reactor.util.retry.Retry;

import java.math.BigDecimal;
import java.net.URI;
import java.time.Duration;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Streams live trade prices from Finnhub's WebSocket and re-broadcasts them to
 * the app (SSE) and the alert engine.
 *
 * One upstream WebSocket connection is shared by all clients (fan-out via a
 * multicast Sink). Symbols are subscribed on demand and re-sent on reconnect.
 * This is the Observer pattern from the design notes made concrete: Finnhub is
 * the subject; the SSE stream and the price-alert checker are the observers.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FinnhubStreamService {

    private final ObjectMapper objectMapper;
    private final WatchlistService watchlistService;

    @Value("${finnhub.api-key}")
    private String apiKey;

    @Value("${finnhub.websocket-url:wss://ws.finnhub.io}")
    private String websocketUrl;

    // Shared hot stream of price ticks (autoCancel=false so it survives client churn)
    private final Sinks.Many<PriceUpdate> priceSink =
        Sinks.many().multicast().onBackpressureBuffer(1024, false);

    // Outbound subscribe/unsubscribe messages to Finnhub
    private final Sinks.Many<String> outboundSink =
        Sinks.many().multicast().onBackpressureBuffer(256, false);

    private final Set<String> subscribedSymbols = ConcurrentHashMap.newKeySet();

    @PostConstruct
    public void connect() {
        if (apiKey == null || apiKey.isBlank()) {
            log.warn("No Finnhub API key configured - live price streaming disabled");
            return;
        }

        ReactorNettyWebSocketClient client = new ReactorNettyWebSocketClient();
        URI uri = URI.create(websocketUrl + "?token=" + apiKey);

        client.execute(uri, session -> {
                // On (re)connect, re-send all currently-subscribed symbols first,
                // then stream any new subscription messages.
                Flux<String> initial = Flux.fromIterable(subscribedSymbols).map(this::subscribeMessage);
                Flux<String> outboundMessages = Flux.concat(initial, outboundSink.asFlux());

                Mono<Void> outbound = session.send(outboundMessages.map(session::textMessage));
                Mono<Void> inbound = session.receive()
                    .map(WebSocketMessage::getPayloadAsText)
                    .doOnNext(this::handleMessage)
                    .then();

                return Mono.zip(outbound, inbound).then();
            })
            .doOnError(e -> log.warn("Finnhub WS error, will retry: {}", e.getMessage()))
            .retryWhen(Retry.backoff(Long.MAX_VALUE, Duration.ofSeconds(5))
                .maxBackoff(Duration.ofSeconds(30)))
            .subscribe();

        log.info("Finnhub WebSocket streaming initialized");
    }

    /**
     * Get a live stream of price updates for the given symbols, subscribing
     * to any not already tracked.
     */
    public Flux<PriceUpdate> stream(Set<String> symbols) {
        symbols.forEach(this::subscribe);
        return priceSink.asFlux().filter(update -> symbols.contains(update.symbol()));
    }

    private void subscribe(String symbol) {
        String upper = symbol.toUpperCase();
        if (subscribedSymbols.add(upper)) {
            outboundSink.tryEmitNext(subscribeMessage(upper));
            log.debug("Subscribed to Finnhub stream: {}", upper);
        }
    }

    private String subscribeMessage(String symbol) {
        return "{\"type\":\"subscribe\",\"symbol\":\"" + symbol + "\"}";
    }

    private void handleMessage(String payload) {
        try {
            JsonNode root = objectMapper.readTree(payload);
            if (!"trade".equals(root.path("type").asText())) {
                return; // ignore ping / other control messages
            }
            for (JsonNode trade : root.path("data")) {
                String symbol = trade.path("s").asText();
                BigDecimal price = trade.path("p").decimalValue();
                long timestamp = trade.path("t").asLong();

                priceSink.tryEmitNext(new PriceUpdate(symbol, price, timestamp));
                // Fire-and-forget alert evaluation off the live price
                watchlistService.checkAlerts(symbol, price).subscribe();
            }
        } catch (Exception e) {
            log.warn("Failed to parse Finnhub message: {}", e.getMessage());
        }
    }
}
