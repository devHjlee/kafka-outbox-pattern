package com.hj.kafkaoutboxpattern.outbox.scheduler;

import com.hj.kafkaoutboxpattern.order.kafka.event.OrderCreatedEvent;
import com.hj.kafkaoutboxpattern.outbox.service.OutBoxPublishService;
import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.event.TransactionPhase;


@Component
@RequiredArgsConstructor
public class OutboxAfterCommitListener {

    private final OutBoxPublishService outBoxPublishService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void publishToKafka(OrderCreatedEvent event) {
        outBoxPublishService.publish(event);
    }
}

