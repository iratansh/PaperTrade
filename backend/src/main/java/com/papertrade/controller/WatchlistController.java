package com.papertrade.controller;

import com.papertrade.domain.WatchlistItem;
import com.papertrade.dto.AddWatchlistRequest;
import com.papertrade.dto.WatchlistResponse;
import com.papertrade.service.WatchlistService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Watchlist + price-alert management for the authenticated user.
 */
@Slf4j
@RestController
@RequestMapping("/api/watchlist")
@RequiredArgsConstructor
public class WatchlistController {

    private final WatchlistService watchlistService;

    @GetMapping
    public Flux<WatchlistResponse> list(Authentication authentication) {
        UUID userId = UUID.fromString(authentication.getName());
        return watchlistService.list(userId).map(this::toResponse);
    }

    @PostMapping
    public Mono<WatchlistResponse> addOrUpdate(Authentication authentication,
                                               @Valid @RequestBody AddWatchlistRequest request) {
        UUID userId = UUID.fromString(authentication.getName());
        return watchlistService.addOrUpdate(userId, request.getSymbol(), request.getAlertPrice())
            .map(this::toResponse);
    }

    @DeleteMapping("/{symbol}")
    public Mono<Void> remove(Authentication authentication, @PathVariable String symbol) {
        UUID userId = UUID.fromString(authentication.getName());
        return watchlistService.remove(userId, symbol);
    }

    private WatchlistResponse toResponse(WatchlistItem item) {
        return new WatchlistResponse(
            item.getSymbol(),
            item.getAlertPrice(),
            item.getAlertDirection(),
            item.isTriggered());
    }
}
