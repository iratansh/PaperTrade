package com.papertrade.service;

import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Abstraction over the order execution queue.
 *
 * Decouples "an order was accepted" from "an order was executed" so fills can be
 * processed asynchronously by a worker. Two implementations:
 *  - {@code LocalOrderQueue} (default): no-op; a scheduled DB poller drains the
 *    PENDING orders table locally.
 *  - {@code SqsOrderQueue} (aws profile): publishes the order id to SQS, and an
 *    SQS consumer executes it - enabling horizontally-scaled worker processes.
 *
 * The orders table remains the source of truth in both cases; the queue only
 * carries "please process order X" signals.
 */
public interface OrderQueue {

    Mono<Void> enqueue(UUID orderId);
}
