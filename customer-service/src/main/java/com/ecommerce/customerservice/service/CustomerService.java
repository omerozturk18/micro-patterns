package com.ecommerce.customerservice.service;

import com.ecommerce.customerservice.client.OrderClient;
import com.ecommerce.customerservice.entity.Customer;
import com.ecommerce.customerservice.repository.CustomerRepository;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CustomerService {
    private final CustomerRepository customerRepository;
    private final OrderClient orderClient;

    public Customer save(Customer customer) {
        Customer saved = customerRepository.save(customer);
        // Event Notification (Simulated by simple print, normally pushed to RabbitMQ)
        System.out.println("EVENT NOTIFICATION: CustomerCreatedEvent emitted for " + saved.getEmail());
        return saved;
    }

    @CircuitBreaker(name = "orderServiceCB", fallbackMethod = "fallbackGetOrders")
    public List<Object> getCustomerOrders() {
        return orderClient.getOrders();
    }

    public List<Object> fallbackGetOrders(Exception e) {
        System.out.println("CIRCUIT BREAKER: Order Service is down. Returning empty list.");
        return Collections.emptyList();
    }
}
