package com.ecommerce.orderservice.event;

import com.ecommerce.orderservice.entity.write.Order;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEvent;

/**
 * Domain Event fired when an Order status is updated in Write DB
 * This triggers the OrderProjector to sync data to Read DB
 */
@Getter
@RequiredArgsConstructor
public class OrderUpdatedEvent extends ApplicationEvent {
    private final Order order;
}
