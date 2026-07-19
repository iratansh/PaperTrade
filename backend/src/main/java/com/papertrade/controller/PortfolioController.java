package com.papertrade.controller;

import com.papertrade.dto.PortfolioResponse;
import com.papertrade.dto.SnapshotPoint;
import com.papertrade.service.PortfolioService;
import com.papertrade.service.PortfolioSnapshotService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * REST controller for portfolio operations.
 *
 * The user is derived from the JWT (Authentication principal), never from a
 * request parameter - a caller can only ever see their own portfolio.
 */
@Slf4j
@RestController
@RequestMapping("/api/portfolio")
@RequiredArgsConstructor
public class PortfolioController {

    private final PortfolioService portfolioService;
    private final PortfolioSnapshotService snapshotService;

    /**
     * Get the authenticated user's portfolio (cash balance + positions + P&L).
     *
     * GET /api/portfolio   (Authorization: Bearer <token>)
     */
    @GetMapping
    public Mono<PortfolioResponse> getPortfolio(Authentication authentication) {
        UUID userId = UUID.fromString(authentication.getName());
        log.info("Fetching portfolio for user: {}", userId);
        return portfolioService.getPortfolio(userId);
    }

    /**
     * Get the authenticated user's portfolio value history for the growth chart.
     *
     * GET /api/portfolio/history
     */
    @GetMapping("/history")
    public Flux<SnapshotPoint> getHistory(Authentication authentication) {
        UUID userId = UUID.fromString(authentication.getName());
        return snapshotService.getHistory(userId);
    }
}
