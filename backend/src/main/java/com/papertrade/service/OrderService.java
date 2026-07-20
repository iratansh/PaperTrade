package com.papertrade.service;

import com.papertrade.domain.Order;
import com.papertrade.domain.Position;
import com.papertrade.domain.TradingAccount;
import com.papertrade.domain.Transaction;
import com.papertrade.domain.enums.OrderSide;
import com.papertrade.domain.enums.OrderStatus;
import com.papertrade.domain.enums.OrderType;
import com.papertrade.domain.enums.TransactionType;
import com.papertrade.dto.OrderResponse;
import com.papertrade.dto.PlaceOrderRequest;
import com.papertrade.exception.AccountNotFoundException;
import com.papertrade.exception.InsufficientFundsException;
import com.papertrade.exception.InsufficientSharesException;
import com.papertrade.exception.OrderNotFoundException;
import com.papertrade.repository.AccountRepository;
import com.papertrade.repository.OrderRepository;
import com.papertrade.repository.PositionRepository;
import com.papertrade.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final AccountRepository accountRepository;
    private final PositionRepository positionRepository;
    private final TransactionRepository transactionRepository;
    private final MarketDataService marketDataService;
    private final MarketHoursService marketHoursService;
    private final TransactionalOperator txOperator;

    /**
     * Place a new order (buy or sell)
     *
     * This method demonstrates:
     * - Pessimistic locking for race condition prevention
     * - Transaction management (@Transactional ensures atomicity)
     * - Idempotency (prevent duplicate orders)
     * - Domain-driven design (business logic in domain objects)
     *
     * @param request The order request
     * @return OrderResponse with execution details
     */
    public Mono<OrderResponse> placeOrder(PlaceOrderRequest request) {
        log.info("Placing {} order for {} shares of {}",
                 request.getSide(), request.getQuantity(), request.getSymbol());

        // Check for duplicate order (idempotency)
        if (request.getIdempotencyKey() != null) {
            return orderRepository.findByIdempotencyKey(request.getIdempotencyKey())
                .map(this::toOrderResponse)
                .switchIfEmpty(Mono.defer(() -> submitOrder(request)));
        }

        return submitOrder(request);
    }

    /**
     * Decide whether to execute an order now or queue it as PENDING.
     *
     * - Market closed          -> queue (a background worker fills it when open).
     * - Market open, MARKET     -> execute immediately at the current price.
     * - Market open, LIMIT met   -> execute at the limit price.
     * - Market open, LIMIT unmet -> queue (rests on the book until the price hits).
     *
     * Later this queue moves to SQS + a dedicated worker; today it's the orders
     * table (status = PENDING) drained by a scheduled worker.
     */
    private Mono<OrderResponse> submitOrder(PlaceOrderRequest request) {
        Order order = buildOrder(request);

        if (!marketHoursService.isMarketOpen()) {
            log.info("Market closed - queuing {} order for {}", order.getSide(), order.getSymbol());
            return queueOrder(order);
        }

        return marketDataService.getCurrentPrice(request.getSymbol())
            .flatMap(price -> {
                boolean executable = order.getType() == OrderType.MARKET || order.canExecuteAtPrice(price);
                if (executable) {
                    return executeExistingOrder(order, price)
                        .as(txOperator::transactional)
                        .map(this::toOrderResponse);
                }
                log.info("Limit condition not met - queuing {} order for {}",
                         order.getSide(), order.getSymbol());
                return queueOrder(order);
            });
    }

    /**
     * Persist an order in PENDING state (queued).
     */
    private Mono<OrderResponse> queueOrder(Order order) {
        order.setStatus(OrderStatus.PENDING);
        return orderRepository.save(order).map(this::toOrderResponse);
    }

    /**
     * Execute a single order (new or previously-queued) inside a transaction.
     * Acquires the pessimistic account lock; the price is already known.
     */
    private Mono<Order> executeExistingOrder(Order order, BigDecimal currentPrice) {
        return accountRepository.findByIdWithLock(order.getAccountId())
            .switchIfEmpty(Mono.error(new AccountNotFoundException(order.getAccountId())))
            .flatMap(account -> order.getSide() == OrderSide.BUY
                ? executeBuyOrder(order, account, currentPrice)
                : executeSellOrder(order, account, currentPrice));
    }

    /**
     * Drain the PENDING order queue - executes any order whose conditions are
     * now met. Called by the scheduled queue worker while the market is open.
     * One failing order never blocks the rest.
     */
    public Mono<Long> processPendingOrders() {
        if (!marketHoursService.isMarketOpen()) {
            return Mono.just(0L);
        }
        return orderRepository.findByStatus(OrderStatus.PENDING)
            .flatMap(this::processQueuedOrder)
            .count();
    }

    private Mono<Order> processQueuedOrder(Order order) {
        return marketDataService.getCurrentPrice(order.getSymbol())
            .flatMap(price -> {
                boolean executable = order.getType() == OrderType.MARKET || order.canExecuteAtPrice(price);
                if (!executable) {
                    return Mono.empty(); // leave PENDING; retried next cycle
                }
                return executeExistingOrder(order, price).as(txOperator::transactional);
            })
            .onErrorResume(error -> {
                log.error("Failed to fill queued order {}: {}", order.getOrderId(), error.getMessage());
                return Mono.empty(); // don't let one bad order stop the queue
            });
    }

    /**
     * Execute a BUY order
     * Steps:
     * 1. Get current market price
     * 2. Validate sufficient funds (with lock held)
     * 3. Debit account balance
     * 4. Create or update position
     * 5. Record transaction
     */
    private Mono<Order> executeBuyOrder(Order order, TradingAccount account, BigDecimal currentPrice) {
        // Determine execution price (price already fetched, no network call here)
        BigDecimal executionPrice = determineExecutionPrice(order, currentPrice);
        BigDecimal totalCost = executionPrice.multiply(order.getQuantity());

        // Validate sufficient funds (critical section - lock held!)
        if (!account.canPlaceOrder(totalCost)) {
            order.setStatus(OrderStatus.REJECTED);
            return orderRepository.save(order)
                .flatMap(savedOrder -> Mono.error(
                    new InsufficientFundsException(
                        String.format("Insufficient funds. Required: $%.2f, Available: $%.2f",
                                      totalCost, account.getBalance()))));
        }

        // Debit account balance
        account.debit(totalCost);

        // Mark order as filled
        order.markFilled(executionPrice, order.getQuantity());

        // Save order, update account, update position, log transaction (all atomic)
        return orderRepository.save(order)
            .flatMap(savedOrder -> accountRepository.save(account)
                .then(updatePositionForBuy(account.getAccountId(),
                                           order.getSymbol(),
                                           order.getQuantity(),
                                           executionPrice))
                .then(recordTransaction(savedOrder, executionPrice, TransactionType.BUY))
                .thenReturn(savedOrder));
    }

    /**
     * Execute a SELL order
     * Steps:
     * 1. Get current market price
     * 2. Validate sufficient shares (with lock on position)
     * 3. Credit account balance
     * 4. Update or remove position
     * 5. Record transaction
     */
    private Mono<Order> executeSellOrder(Order order, TradingAccount account, BigDecimal currentPrice) {
        BigDecimal executionPrice = determineExecutionPrice(order, currentPrice);

        // Acquire PESSIMISTIC LOCK on position (prevent overselling)
        return positionRepository.findByAccountIdAndSymbolWithLock(
                account.getAccountId(), order.getSymbol())
            .switchIfEmpty(Mono.error(
                new InsufficientSharesException("No position found for " + order.getSymbol())))
            .flatMap(position -> {
                // Validate sufficient shares (critical section - lock held!)
                if (position.getQuantity().compareTo(order.getQuantity()) < 0) {
                    order.setStatus(OrderStatus.REJECTED);
                    return orderRepository.save(order)
                        .flatMap(savedOrder -> Mono.error(
                            new InsufficientSharesException(
                                String.format("Insufficient shares. Required: %.4f, Available: %.4f",
                                              order.getQuantity(), position.getQuantity()))));
                }

                // Calculate proceeds
                BigDecimal proceeds = executionPrice.multiply(order.getQuantity());

                // Credit account balance
                account.credit(proceeds);

                // Mark order as filled
                order.markFilled(executionPrice, order.getQuantity());

                // Update position (reduce shares)
                position.removeShares(order.getQuantity());

                // Save everything atomically
                return orderRepository.save(order)
                    .flatMap(savedOrder -> accountRepository.save(account)
                        .then(saveOrDeletePosition(position))
                        .then(recordTransaction(savedOrder, executionPrice, TransactionType.SELL))
                        .thenReturn(savedOrder));
            });
    }

    /**
     * Update position after a buy (add shares to existing or create new position)
     */
    private Mono<Position> updatePositionForBuy(UUID accountId, String symbol,
                                                BigDecimal quantity, BigDecimal price) {
        return positionRepository.findByAccountIdAndSymbol(accountId, symbol)
            .flatMap(existingPosition -> {
                // Update existing position
                existingPosition.addShares(quantity, price);
                existingPosition.setCurrentPrice(price);
                return positionRepository.save(existingPosition);
            })
            .switchIfEmpty(Mono.defer(() -> {
                // Create new position
                Position newPosition = Position.builder()
                    // positionId generated by the database (INSERT on null @Id)
                    .accountId(accountId)
                    .symbol(symbol)
                    .quantity(quantity)
                    .averageCost(price)
                    .currentPrice(price)
                    .createdAt(LocalDateTime.now())
                    .build();
                return positionRepository.save(newPosition);
            }));
    }

    /**
     * Save position if shares remain, delete if quantity is 0
     */
    private Mono<Void> saveOrDeletePosition(Position position) {
        if (position.getQuantity().compareTo(BigDecimal.ZERO) == 0) {
            return positionRepository.deleteById(position.getPositionId());
        }
        return positionRepository.save(position).then();
    }

    /**
     * Record transaction in audit log (immutable)
     */
    private Mono<Transaction> recordTransaction(Order order, BigDecimal executionPrice,
                                                TransactionType type) {
        Transaction transaction = Transaction.builder()
            // transactionId generated by the database (INSERT on null @Id)
            .orderId(order.getOrderId())
            .accountId(order.getAccountId())
            .symbol(order.getSymbol())
            .type(type)
            .quantity(order.getFilledQuantity())
            .price(executionPrice)
            .fees(BigDecimal.ZERO) // No fees for now
            .timestamp(LocalDateTime.now())
            .build();

        return transactionRepository.save(transaction);
    }

    /**
     * Determine execution price based on order type
     */
    private BigDecimal determineExecutionPrice(Order order, BigDecimal currentPrice) {
        if (order.getType() == OrderType.MARKET) {
            return currentPrice; // Market orders execute at current price
        }

        // Limit orders: check if price condition is met
        if (order.canExecuteAtPrice(currentPrice)) {
            return order.getLimitPrice(); // Execute at limit price
        }

        // Price condition not met - reject for now (in production, queue the order)
        throw new IllegalStateException("Limit order price condition not met");
    }

    /**
     * Build Order entity from request
     */
    private Order buildOrder(PlaceOrderRequest request) {
        return Order.builder()
            // orderId is generated by the database (DEFAULT gen_random_uuid()).
            // Leaving @Id null lets Spring Data R2DBC treat this as a new row (INSERT).
            .accountId(request.getAccountId())
            .symbol(request.getSymbol().toUpperCase())
            .type(request.getType())
            .side(request.getSide())
            .quantity(request.getQuantity())
            .limitPrice(request.getLimitPrice())
            .status(OrderStatus.PENDING)
            .createdAt(LocalDateTime.now())
            .idempotencyKey(request.getIdempotencyKey())
            .build();
    }

    /**
     * Get order history for an account
     */
    public Flux<OrderResponse> getOrderHistory(UUID accountId) {
        return orderRepository.findByAccountIdOrderByCreatedAtDesc(accountId)
            .map(this::toOrderResponse);
    }

    /**
     * Cancel a pending order
     */
    @Transactional
    public Mono<OrderResponse> cancelOrder(UUID orderId, UUID accountId) {
        return orderRepository.findById(orderId)
            .switchIfEmpty(Mono.error(new OrderNotFoundException(orderId)))
            .flatMap(order -> {
                // Verify order belongs to this account
                if (!order.getAccountId().equals(accountId)) {
                    return Mono.error(new IllegalArgumentException("Order does not belong to this account"));
                }

                order.cancel();
                return orderRepository.save(order);
            })
            .map(this::toOrderResponse);
    }

    /**
     * Convert Order entity to OrderResponse DTO
     */
    private OrderResponse toOrderResponse(Order order) {
        return OrderResponse.builder()
            .orderId(order.getOrderId())
            .symbol(order.getSymbol())
            .type(order.getType())
            .side(order.getSide())
            .quantity(order.getQuantity())
            .limitPrice(order.getLimitPrice())
            .status(order.getStatus())
            .filledPrice(order.getFilledPrice())
            .filledQuantity(order.getFilledQuantity())
            .createdAt(order.getCreatedAt())
            .filledAt(order.getFilledAt())
            .build();
    }
}
