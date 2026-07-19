package com.papertrade.controller;

import com.papertrade.dto.OrderResponse;
import com.papertrade.dto.PlaceOrderRequest;
import com.papertrade.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * REST controller for order-related operations
 *
 * Endpoints:
 * - POST /api/orders - Place a new order
 * - GET /api/orders - Get order history
 * - DELETE /api/orders/{orderId} - Cancel an order
 */
@Slf4j
@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    /**
     * Place a new order (market or limit, buy or sell)
     *
     * Example request:
     * POST /api/orders
     * {
     *   "accountId": "uuid-here",
     *   "symbol": "AAPL",
     *   "type": "MARKET",
     *   "side": "BUY",
     *   "quantity": 10,
     *   "idempotencyKey": "unique-key-123"
     * }
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<OrderResponse> placeOrder(@Valid @RequestBody PlaceOrderRequest request) {
        log.info("Received order request: {} {} shares of {}",
                 request.getSide(), request.getQuantity(), request.getSymbol());
        return orderService.placeOrder(request);
    }

    /**
     * Get order history for an account
     *
     * GET /api/orders?accountId=uuid-here
     */
    @GetMapping
    public Flux<OrderResponse> getOrderHistory(@RequestParam UUID accountId) {
        log.info("Fetching order history for account: {}", accountId);
        return orderService.getOrderHistory(accountId);
    }

    /**
     * Cancel a pending order
     *
     * DELETE /api/orders/{orderId}?accountId=uuid-here
     */
    @DeleteMapping("/{orderId}")
    public Mono<OrderResponse> cancelOrder(@PathVariable UUID orderId,
                                           @RequestParam UUID accountId) {
        log.info("Cancelling order: {} for account: {}", orderId, accountId);
        return orderService.cancelOrder(orderId, accountId);
    }
}
