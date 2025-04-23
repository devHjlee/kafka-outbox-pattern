package com.hj.kafkaoutboxpattern.outbox.scheduler;

import com.hj.kafkaoutboxpattern.order.kafka.event.OrderCreatedEvent;
import com.hj.kafkaoutboxpattern.outbox.enity.EventOutbox;
import com.hj.kafkaoutboxpattern.outbox.repository.EventOutboxRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.event.TransactionPhase;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class OutboxBeforeCommitListener {

    private final EventOutboxRepository outboxRepository;

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void saveOutbox(OrderCreatedEvent event) {
        EventOutbox outbox = EventOutbox.builder()
                .aggregateType("Order")
                .aggregateId(event.getOrderId())
                .type("OrderCreated")
                .payload(toJson(event))
                .createdAt(LocalDateTime.now())
                .sent(false)
                .build();

        outboxRepository.save(outbox);
    }

    private String toJson(OrderCreatedEvent event) {
        return String.format(
                "{\"orderId\":%d,\"productId\":%d,\"quantity\":%d,\"createdAt\":\"%s\"}",
                event.getOrderId(), event.getProductId(), event.getQuantity(), event.getCreatedAt()
        );
    }
}
