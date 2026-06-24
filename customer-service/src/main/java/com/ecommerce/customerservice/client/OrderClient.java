package com.ecommerce.customerservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import java.util.List;

@FeignClient(name = "order-service")
public interface OrderClient {
    @GetMapping("/api/orders/queries")
    List<Object> getOrders();
}
