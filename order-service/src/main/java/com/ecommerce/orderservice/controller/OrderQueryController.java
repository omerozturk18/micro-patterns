package com.ecommerce.orderservice.controller;

import com.ecommerce.orderservice.entity.read.OrderReadModel;
import com.ecommerce.orderservice.service.OrderQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * OrderQueryController
 * REST endpoints for read operations (Queries)
 * Returns Read Model (OrderReadModel from Read DB)
 */
@RestController
@RequestMapping("/api/orders/queries")
@RequiredArgsConstructor
public class OrderQueryController {
    private final OrderQueryService queryService;

    /**
     * GET /api/orders/queries
     * Retrieves all orders
     */
    @GetMapping
    public List<OrderReadModel> getAll() {
        return queryService.getAllOrders();
    }

    /**
     * GET /api/orders/queries/{id}
     * Retrieves a specific order by ID
     */
    @GetMapping("/{id}")
    public OrderReadModel getById(@PathVariable String id) {
        return queryService.getOrder(id);
    }

    /**
     * GET /api/orders/queries/status/{status}
     * Retrieves orders by status
     */
    @GetMapping("/status/{status}")
    public List<OrderReadModel> getByStatus(@PathVariable String status) {
        return queryService.getOrdersByStatus(status);
    }

    /**
     * GET /api/orders/queries/product/{productId}
     * Retrieves orders by product ID
     */
    @GetMapping("/product/{productId}")
    public List<OrderReadModel> getByProduct(@PathVariable Long productId) {
        return queryService.getOrdersByProduct(productId);
    }
}
