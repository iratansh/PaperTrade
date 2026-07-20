package com.papertrade.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Drains the PENDING order queue on a schedule while the market is open.
 *
 * This is the LOCAL execution driver (active when the "aws" profile is off).
 * In the cloud, SQS + {@code SqsOrderConsumer} drive execution instead.
 */
@Slf4j
@Component
@Profile("!aws")
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
