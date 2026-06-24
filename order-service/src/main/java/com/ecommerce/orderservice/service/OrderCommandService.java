package com.ecommerce.orderservice.service;

import com.ecommerce.orderservice.dto.OrderRequest;
import com.ecommerce.orderservice.entity.write.Order;
import com.ecommerce.orderservice.entity.write.OutboxEvent;
import com.ecommerce.orderservice.event.OrderCreatedEvent;
import com.ecommerce.orderservice.event.OrderUpdatedEvent;
import com.ecommerce.orderservice.repository.write.OrderWriteRepository;
import com.ecommerce.orderservice.repository.write.OutboxEventWriteRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.UUID;

/**
 * OrderCommandService
 * Handles all write operations (Commands) for Orders
 * Writes to Write DB and publishes domain events for projection to Read DB
 * Part of the CQRS Command side
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderCommandService {
    private final OrderWriteRepository orderWriteRepository;
    private final OutboxEventWriteRepository outboxEventWriteRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final SagaOrchestrator sagaOrchestrator;

    /**
     * Creates a new order in the Write DB and publishes events for synchronization
     * The OrderProjector will listen to OrderCreatedEvent and sync to Read DB
     */
    @Transactional(transactionManager = "writeTransactionManager")
    public Order createOrder(OrderRequest request) {
        log.info("Creating order for product: {}, amount: {}", request.productId(), request.amount());
        
        // Create order in Write DB
        Order order = new Order(UUID.randomUUID().toString(), request.productId(), request.amount(), "PENDING");
        Order savedOrder = orderWriteRepository.save(order);
        
        // Outbox Pattern - Store event in Write DB for reliability
        OutboxEvent outboxEvent = new OutboxEvent(
            UUID.randomUUID().toString(),
            "ORDER_CREATED",
            "{\"orderId\":\"" + savedOrder.getId() + "\",\"status\":\"" + savedOrder.getStatus() + "\"}",
            false
        );
        outboxEventWriteRepository.save(outboxEvent);
        
        // Publish domain event for immediate projection
        eventPublisher.publishEvent(new OrderCreatedEvent(savedOrder));
        
        log.info("Order created successfully with ID: {}", savedOrder.getId());
        
        // Trigger Saga for external services
        sagaOrchestrator.processOrder(savedOrder);
        
        return savedOrder;
    }

    /**
     * Updates order status in Write DB and publishes update events
     */
    @Transactional(transactionManager = "writeTransactionManager")
    public Order updateOrderStatus(String orderId, String newStatus) {
        log.info("Updating order {} status to {}", orderId, newStatus);
        
        Order order = orderWriteRepository.findById(orderId)
            .orElseThrow(() -> new RuntimeException("Order not found with ID: " + orderId));
        
        order.setStatus(newStatus);
        Order updatedOrder = orderWriteRepository.save(order);
        
        // Record event in Outbox
        OutboxEvent outboxEvent = new OutboxEvent(
            UUID.randomUUID().toString(),
            "ORDER_UPDATED",
            "{\"orderId\":\"" + updatedOrder.getId() + "\",\"status\":\"" + updatedOrder.getStatus() + "\"}",
            false
        );
        outboxEventWriteRepository.save(outboxEvent);
        
        // Publish domain event for projection
        eventPublisher.publishEvent(new OrderUpdatedEvent(updatedOrder));
        
        log.info("Order {} status updated successfully", orderId);
        
        return updatedOrder;
    }
}
