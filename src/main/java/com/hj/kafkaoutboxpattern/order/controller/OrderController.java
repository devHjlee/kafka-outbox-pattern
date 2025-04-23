package com.hj.kafkaoutboxpattern.order.controller;

import com.hj.kafkaoutboxpattern.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    public ResponseEntity<Long> createOrder(@RequestParam Long productId,
                                            @RequestParam int quantity) {
        Long orderId = orderService.createOrder(productId, quantity);
        return ResponseEntity.ok(orderId);
    }
}
