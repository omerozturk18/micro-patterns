package com.ecommerce.orderservice.controller;

import com.ecommerce.orderservice.entity.Order;
import com.ecommerce.orderservice.service.OrderQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/orders/queries")
@RequiredArgsConstructor
public class OrderQueryController {
    private final OrderQueryService queryService;

    @GetMapping
    public List<Order> getAll() {
        return queryService.getAllOrders();
    }
}
