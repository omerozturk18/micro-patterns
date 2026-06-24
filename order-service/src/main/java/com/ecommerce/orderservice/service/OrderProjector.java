package com.ecommerce.orderservice.service;

import com.ecommerce.orderservice.entity.read.OrderReadModel;
import com.ecommerce.orderservice.entity.write.Order;
import com.ecommerce.orderservice.event.OrderCreatedEvent;
import com.ecommerce.orderservice.event.OrderUpdatedEvent;
import com.ecommerce.orderservice.repository.read.OrderReadRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;

/**
 * OrderProjector
 * Implements the Projection pattern in CQRS
 * Listens to domain events from Write DB and updates the Read DB asynchronously
 * This achieves Eventual Consistency between Write and Read models
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderProjector {
    private final OrderReadRepository orderReadRepository;

    /**
     * When an OrderCreatedEvent is published, this handler projects it to the Read DB
     * Runs asynchronously with @Async if configured, or synchronously by default
     */
    @EventListener
    @Transactional
    public void handleOrderCreated(OrderCreatedEvent event) {
        log.info("Projecting OrderCreatedEvent for Order ID: {}", event.getOrder().getId());
        
        Order writeOrder = event.getOrder();
        
        OrderReadModel readModel = new OrderReadModel();
        readModel.setId(writeOrder.getId());
        readModel.setProductId(writeOrder.getProductId());
        readModel.setProductName("Product-" + writeOrder.getProductId()); // Simplified denormalization
        readModel.setAmount(writeOrder.getAmount());
        readModel.setStatus(writeOrder.getStatus());
        readModel.setCreatedAt(writeOrder.getCreatedAt());
        readModel.setUpdatedAt(writeOrder.getUpdatedAt());
        readModel.setProjectedAt(LocalDateTime.now());
        
        orderReadRepository.save(readModel);
        log.info("OrderReadModel projected successfully for Order ID: {}", writeOrder.getId());
    }

    /**
     * When an OrderUpdatedEvent is published, this handler updates the projection in Read DB
     */
    @EventListener
    @Transactional
    public void handleOrderUpdated(OrderUpdatedEvent event) {
        log.info("Projecting OrderUpdatedEvent for Order ID: {}", event.getOrder().getId());
        
        Order writeOrder = event.getOrder();
        
        OrderReadModel readModel = orderReadRepository.findById(writeOrder.getId())
            .orElseThrow(() -> new RuntimeException("OrderReadModel not found for ID: " + writeOrder.getId()));
        
        readModel.setProductId(writeOrder.getProductId());
        readModel.setAmount(writeOrder.getAmount());
        readModel.setStatus(writeOrder.getStatus());
        readModel.setUpdatedAt(writeOrder.getUpdatedAt());
        readModel.setProjectedAt(LocalDateTime.now());
        
        orderReadRepository.save(readModel);
        log.info("OrderReadModel updated successfully for Order ID: {}", writeOrder.getId());
    }
}
