package com.ecommerce.orderservice.controller;

import com.ecommerce.orderservice.dto.OrderRequest;
import com.ecommerce.orderservice.entity.Order;
import com.ecommerce.orderservice.service.OrderCommandService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/orders/commands")
@RequiredArgsConstructor
public class OrderCommandController {
    private final OrderCommandService commandService;

    @PostMapping
    public Order createOrder(@RequestBody OrderRequest request) {
        return commandService.createOrder(request);
    }
}
