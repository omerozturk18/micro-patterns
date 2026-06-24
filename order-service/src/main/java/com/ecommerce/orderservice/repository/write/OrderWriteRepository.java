package com.ecommerce.orderservice.repository.write;

import com.ecommerce.orderservice.entity.write.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OrderWriteRepository extends JpaRepository<Order, String> {
}
