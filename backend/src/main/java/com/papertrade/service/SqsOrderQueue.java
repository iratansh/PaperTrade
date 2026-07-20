package com.papertrade.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

import java.util.UUID;

/**
 * SQS-backed order queue (aws profile). Publishes the order id so a worker
 * (possibly a different instance) can execute the fill asynchronously.
 */
@Slf4j
@Component
@Profile("aws")
public class SqsOrderQueue implements OrderQueue {

    private final SqsAsyncClient sqs;
    private final String queueUrl;

    public SqsOrderQueue(SqsAsyncClient sqs, @Value("${aws.sqs.order-queue-url}") String queueUrl) {
        this.sqs = sqs;
        this.queueUrl = queueUrl;
    }

    @Override
    public Mono<Void> enqueue(UUID orderId) {
        SendMessageRequest request = SendMessageRequest.builder()
            .queueUrl(queueUrl)
            .messageBody(orderId.toString())
            .build();
        return Mono.fromFuture(sqs.sendMessage(request))
            .doOnSuccess(r -> log.debug("Enqueued order {} to SQS", orderId))
            .doOnError(e -> log.error("Failed to enqueue order {}: {}", orderId, e.getMessage()))
            .then();
    }
}
