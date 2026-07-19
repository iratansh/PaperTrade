package com.papertrade.controller;

import com.papertrade.dto.OrderResponse;
import com.papertrade.dto.PlaceOrderRequest;
import com.papertrade.exception.AccountNotFoundException;
import com.papertrade.service.AccountService;
import com.papertrade.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * REST controller for order operations.
 *
 * The account is always resolved from the authenticated user's JWT - the client
 * never supplies an accountId, so a user can only trade in their own account.
 */
@Slf4j
@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;
    private final AccountService accountService;

    /**
     * Place a new order (market/limit, buy/sell) for the authenticated user.
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<OrderResponse> placeOrder(Authentication authentication,
                                          @Valid @RequestBody PlaceOrderRequest request) {
        return resolveAccountId(authentication)
            .flatMap(accountId -> {
                request.setAccountId(accountId); // server-authoritative, ignore any client value
                log.info("Received {} order: {} shares of {}",
                         request.getSide(), request.getQuantity(), request.getSymbol());
                return orderService.placeOrder(request);
            });
    }

    /**
     * Get order history for the authenticated user's account.
     */
    @GetMapping
    public Flux<OrderResponse> getOrderHistory(Authentication authentication) {
        return resolveAccountId(authentication)
            .flatMapMany(orderService::getOrderHistory);
    }

    /**
     * Cancel a pending order in the authenticated user's account.
     */
    @DeleteMapping("/{orderId}")
    public Mono<OrderResponse> cancelOrder(Authentication authentication,
                                           @PathVariable UUID orderId) {
        return resolveAccountId(authentication)
            .flatMap(accountId -> orderService.cancelOrder(orderId, accountId));
    }

    /**
     * Resolve the authenticated user's account id from the JWT principal.
     */
    private Mono<UUID> resolveAccountId(Authentication authentication) {
        UUID userId = UUID.fromString(authentication.getName());
        return accountService.getAccountByUserId(userId)
            .switchIfEmpty(Mono.error(new AccountNotFoundException("No account for user: " + userId)))
            .map(account -> account.getAccountId());
    }
}
