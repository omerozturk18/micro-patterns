package com.ecommerce.orderservice.controller;

import com.ecommerce.orderservice.dto.OrderRequest;
import com.ecommerce.orderservice.entity.write.Order;
import com.ecommerce.orderservice.service.OrderCommandService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * OrderCommandController
 * REST endpoints for write operations (Commands)
 * Returns Write Model (Order from Write DB)
 */
@RestController
@RequestMapping("/api/orders/commands")
@RequiredArgsConstructor
public class OrderCommandController {
    private final OrderCommandService commandService;

    /**
     * POST /api/orders/commands
     * Creates a new order
     */
    @PostMapping
    public Order createOrder(@RequestBody OrderRequest request) {
        return commandService.createOrder(request);
    }

    /**
     * PUT /api/orders/commands/{id}/status
     * Updates order status
     */
    @PutMapping("/{id}/status")
    public Order updateOrderStatus(@PathVariable String id, @RequestParam String status) {
        return commandService.updateOrderStatus(id, status);
    }
}
