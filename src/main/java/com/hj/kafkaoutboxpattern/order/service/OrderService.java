package com.hj.kafkaoutboxpattern.order.service;

import com.hj.kafkaoutboxpattern.order.entity.Order;
import com.hj.kafkaoutboxpattern.order.entity.OrderStatus;
import com.hj.kafkaoutboxpattern.order.kafka.event.OrderCreatedEvent;
import com.hj.kafkaoutboxpattern.order.repository.OrderRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final ApplicationEventPublisher publisher;

    @Transactional
    public Long createOrder(Long productId, int quantity) {
        Order order = Order.builder()
                .productId(productId)
                .quantity(quantity)
                .status(OrderStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .build();

        Order orderSave = orderRepository.save(order);

        OrderCreatedEvent event = new OrderCreatedEvent(
                orderSave.getId(), orderSave.getProductId(), orderSave.getQuantity(), orderSave.getCreatedAt());

        // Spring 이벤트 발행 : BEFORE_COMMIT / AFTER_COMMIT 리스너에서 각각 처리
        publisher.publishEvent(event);

        return orderSave.getId();
    }
}
