package com.hj.kafkaoutboxpattern.order.repository;

import com.hj.kafkaoutboxpattern.order.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderRepository extends JpaRepository<Order, Long> {
}
