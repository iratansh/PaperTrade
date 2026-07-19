package com.papertrade.controller;

import com.papertrade.dto.PortfolioResponse;
import com.papertrade.service.PortfolioService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * REST controller for portfolio operations
 *
 * Endpoints:
 * - GET /api/portfolio - Get complete portfolio summary
 */
@Slf4j
@RestController
@RequestMapping("/api/portfolio")
@RequiredArgsConstructor
public class PortfolioController {

    private final PortfolioService portfolioService;

    /**
     * Get complete portfolio for a user
     *
     * Returns:
     * - Cash balance
     * - All positions with unrealized P&L
     * - Total portfolio value
     *
     * GET /api/portfolio?userId=uuid-here
     */
    @GetMapping
    public Mono<PortfolioResponse> getPortfolio(@RequestParam UUID userId) {
        log.info("Fetching portfolio for user: {}", userId);
        return portfolioService.getPortfolio(userId);
    }
}
