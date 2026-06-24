package com.ecommerce.orderservice.service;

import com.ecommerce.orderservice.dto.OrderRequest;
import com.ecommerce.orderservice.entity.Order;
import com.ecommerce.orderservice.entity.OutboxEvent;
import com.ecommerce.orderservice.repository.OrderRepository;
import com.ecommerce.orderservice.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OrderCommandService {
    private final OrderRepository orderRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final SagaOrchestrator sagaOrchestrator;

    @Transactional
    public Order createOrder(OrderRequest request) {
        Order order = new Order(UUID.randomUUID().toString(), request.productId(), request.amount(), "PENDING");
        Order savedOrder = orderRepository.save(order);

        // Outbox Pattern
        OutboxEvent event = new OutboxEvent(
            UUID.randomUUID().toString(),
            "ORDER_CREATED",
            "{\"orderId\":\"" + savedOrder.getId() + "\"}",
            false
        );
        outboxEventRepository.save(event);

        // Trigger Saga
        sagaOrchestrator.processOrder(savedOrder);

        return savedOrder;
    }
}
