package com.hj.kafkaoutboxpattern.order.kafka.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderCreatedEvent {
    private Long orderId;
    private Long productId;
    private int quantity;
    private LocalDateTime createdAt;
}
