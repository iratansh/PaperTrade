package com.papertrade.service;

import com.papertrade.domain.enums.OrderStatus;
import com.papertrade.repository.OrderRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageResponse;

import java.util.UUID;

/**
 * Consumes order ids from SQS and executes them (aws profile).
 *
 * Long-polls the queue and hands each order to {@link OrderService#processSingle}.
 * A separate re-drive re-enqueues still-PENDING orders while the market is open,
 * so orders queued while closed (or resting limit orders) get another chance.
 */
@Slf4j
@Component
@Profile("aws")
public class SqsOrderConsumer {

    private final SqsAsyncClient sqs;
    private final String queueUrl;
    private final OrderService orderService;
    private final OrderRepository orderRepository;
    private final MarketHoursService marketHoursService;
    private final OrderQueue orderQueue;

    public SqsOrderConsumer(SqsAsyncClient sqs,
                            @Value("${aws.sqs.order-queue-url}") String queueUrl,
                            OrderService orderService,
                            OrderRepository orderRepository,
                            MarketHoursService marketHoursService,
                            OrderQueue orderQueue) {
        this.sqs = sqs;
        this.queueUrl = queueUrl;
        this.orderService = orderService;
        this.orderRepository = orderRepository;
        this.marketHoursService = marketHoursService;
        this.orderQueue = orderQueue;
    }

    @Scheduled(fixedDelayString = "${aws.sqs.poll-delay-ms:1000}")
    public void poll() {
        ReceiveMessageRequest request = ReceiveMessageRequest.builder()
            .queueUrl(queueUrl)
            .maxNumberOfMessages(10)
            .waitTimeSeconds(10) // long poll
            .build();

        Mono.fromFuture(sqs.receiveMessage(request))
            .flatMapIterable(ReceiveMessageResponse::messages)
            .flatMap(this::handleMessage)
            .subscribe(
                null,
                error -> log.error("SQS poll failed: {}", error.getMessage()));
    }

    private Mono<Void> handleMessage(Message message) {
        UUID orderId;
        try {
            orderId = UUID.fromString(message.body());
        } catch (IllegalArgumentException e) {
            return deleteMessage(message); // malformed - drop it
        }
        return orderService.processSingle(orderId)
            .then(deleteMessage(message));
    }

    private Mono<Void> deleteMessage(Message message) {
        DeleteMessageRequest request = DeleteMessageRequest.builder()
            .queueUrl(queueUrl)
            .receiptHandle(message.receiptHandle())
            .build();
        return Mono.fromFuture(sqs.deleteMessage(request)).then();
    }

    /**
     * Re-enqueue orders that are still PENDING while the market is open, so
     * closed-market queues and resting limit orders get re-evaluated.
     */
    @Scheduled(fixedDelayString = "${aws.sqs.redrive-delay-ms:30000}")
    public void redrivePending() {
        if (!marketHoursService.isMarketOpen()) {
            return;
        }
        orderRepository.findByStatus(OrderStatus.PENDING)
            .flatMap(order -> orderQueue.enqueue(order.getOrderId()))
            .subscribe(
                null,
                error -> log.error("Order re-drive failed: {}", error.getMessage()));
    }
}
