package com.ecommerce.orderservice.service;

import com.ecommerce.orderservice.client.PaymentClient;
import com.ecommerce.orderservice.client.PaymentRequest;
import com.ecommerce.orderservice.client.PaymentResponse;
import com.ecommerce.orderservice.entity.write.Order;
import com.ecommerce.orderservice.repository.write.OrderWriteRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ecommerce.orderservice.event.OrderUpdatedEvent;

/**
 * SagaOrchestrator
 * Implements the Saga Pattern for distributed transactions
 * Orchestrates multi-step order processing across services (Payment, etc.)
 * Updates the Write DB and publishes events for projection
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SagaOrchestrator {
    private final PaymentClient paymentClient;
    private final OrderWriteRepository orderWriteRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional(transactionManager = "writeTransactionManager")
    public void processOrder(Order order) {
        log.info("SAGA: Starting payment for order {}", order.getId());
        try {
            PaymentResponse response = paymentClient.processPayment(
                new PaymentRequest(order.getId(), order.getAmount())
            );
            
            if ("SUCCESS".equals(response.status())) {
                order.setStatus("COMPLETED");
                log.info("SAGA: Payment Successful. Order COMPLETED.");
            } else {
                order.setStatus("CANCELLED");
                log.warn("SAGA: Payment Failed. Order CANCELLED. Reason: {}", response.message());
            }
        } catch (Exception e) {
            order.setStatus("CANCELLED");
            log.error("SAGA: Payment Service Down. Compensating Transaction... Order CANCELLED.", e);
        }
        
        Order updatedOrder = orderWriteRepository.save(order);
        
        // Publish event for projection to Read DB
        eventPublisher.publishEvent(new OrderUpdatedEvent(updatedOrder));
    }
}
