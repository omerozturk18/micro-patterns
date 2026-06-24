package com.ecommerce.orderservice.service;

import com.ecommerce.orderservice.client.PaymentClient;
import com.ecommerce.orderservice.client.PaymentRequest;
import com.ecommerce.orderservice.client.PaymentResponse;
import com.ecommerce.orderservice.entity.Order;
import com.ecommerce.orderservice.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SagaOrchestrator {
    private final PaymentClient paymentClient;
    private final OrderRepository orderRepository;

    public void processOrder(Order order) {
        System.out.println("SAGA: Starting payment for order " + order.getId());
        try {
            PaymentResponse response = paymentClient.processPayment(new PaymentRequest(order.getId(), order.getAmount()));
            
            if ("SUCCESS".equals(response.status())) {
                order.setStatus("COMPLETED");
                System.out.println("SAGA: Payment Successful. Order COMPLETED.");
            } else {
                order.setStatus("CANCELLED");
                System.out.println("SAGA: Payment Failed. Order CANCELLED. Reason: " + response.message());
            }
        } catch (Exception e) {
            order.setStatus("CANCELLED");
            System.out.println("SAGA: Payment Service Down. Compensating Transaction... Order CANCELLED.");
        }
        orderRepository.save(order);
    }
}
