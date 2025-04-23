package com.hj.kafkaoutboxpattern.order.service;

import com.hj.kafkaoutboxpattern.outbox.enity.EventOutbox;
import com.hj.kafkaoutboxpattern.outbox.repository.EventOutboxRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;


import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class OrderServiceTest {
    @Autowired
    private OrderService orderService;

    @Autowired
    private EventOutboxRepository outboxRepository;

    @Test
    void 주문생성과_아웃박스_이벤트_정상처리() throws InterruptedException {
        // given
        Long productId = 1L;
        int quantity = 2;

        // when
        Long orderId = orderService.createOrder(productId, quantity);

        // then
        List<EventOutbox> outboxes = outboxRepository.findAll();
        assertThat(outboxes).hasSize(1);

        Thread.sleep(1000); // Kafka 전송 및 sent=true 업데이트를 기다림

        EventOutbox saved = outboxRepository.findById(outboxes.get(0).getId()).orElseThrow();
        assertThat(saved.isSent()).isTrue();
    }
}