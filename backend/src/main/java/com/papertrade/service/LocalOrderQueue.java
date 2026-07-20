package com.papertrade.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Local order queue: a no-op.
 *
 * Orders are saved PENDING to the database and the scheduled {@link OrderQueueWorker}
 * polls and executes them. Active whenever the "aws" profile is NOT enabled.
 */
@Slf4j
@Component
@Profile("!aws")
public class LocalOrderQueue implements OrderQueue {

    @Override
    public Mono<Void> enqueue(UUID orderId) {
        // No external queue locally - the DB poller (OrderQueueWorker) drains PENDING orders.
        log.debug("Order {} queued locally (DB poller will drain it)", orderId);
        return Mono.empty();
    }
}
