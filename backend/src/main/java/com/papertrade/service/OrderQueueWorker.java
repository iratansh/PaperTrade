package com.papertrade.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Drains the PENDING order queue on a schedule while the market is open.
 *
 * This is a placeholder for the eventual SQS + background-worker setup: the
 * queue is the orders table (status = PENDING) and this scheduled poller is the
 * worker. Swapping to SQS later means replacing this class, not the order logic.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderQueueWorker {

    private final OrderService orderService;

    @Scheduled(
        initialDelayString = "${order-queue.initial-delay-ms:20000}",
        fixedDelayString = "${order-queue.interval-ms:30000}")
    public void drainQueue() {
        orderService.processPendingOrders()
            .subscribe(
                filled -> {
                    if (filled > 0) log.info("Filled {} queued order(s)", filled);
                },
                error -> log.error("Order queue worker failed: {}", error.getMessage()));
    }
}
