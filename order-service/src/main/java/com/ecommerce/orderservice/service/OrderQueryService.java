package com.ecommerce.orderservice.service;

import com.ecommerce.orderservice.entity.read.OrderReadModel;
import com.ecommerce.orderservice.repository.read.OrderReadRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

/**
 * OrderQueryService
 * Handles all read operations (Queries) for Orders
 * Reads from Read DB (denormalized read model)
 * Part of the CQRS Query side
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderQueryService {
    private final OrderReadRepository orderReadRepository;

    /**
     * Get all orders from Read DB
     * Fast read operation on denormalized data
     */
    @Transactional(transactionManager = "readTransactionManager", readOnly = true)
    public List<OrderReadModel> getAllOrders() {
        log.info("Fetching all orders from Read DB");
        return orderReadRepository.findAll();
    }

    /**
     * Get a specific order by ID from Read DB
     */
    @Transactional(transactionManager = "readTransactionManager", readOnly = true)
    public OrderReadModel getOrder(String id) {
        log.info("Fetching order {} from Read DB", id);
        return orderReadRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Order not found with ID: " + id));
    }

    /**
     * Get orders by status from Read DB
     * Optimized query on denormalized data
     */
    @Transactional(transactionManager = "readTransactionManager", readOnly = true)
    public List<OrderReadModel> getOrdersByStatus(String status) {
        log.info("Fetching orders with status {} from Read DB", status);
        return orderReadRepository.findByStatus(status);
    }

    /**
     * Get orders by product ID from Read DB
     */
    @Transactional(transactionManager = "readTransactionManager", readOnly = true)
    public List<OrderReadModel> getOrdersByProduct(Long productId) {
        log.info("Fetching orders for product {} from Read DB", productId);
        return orderReadRepository.findByProductId(productId);
    }
}
