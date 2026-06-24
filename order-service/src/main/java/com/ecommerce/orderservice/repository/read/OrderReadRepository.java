package com.ecommerce.orderservice.repository.read;

import com.ecommerce.orderservice.entity.read.OrderReadModel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface OrderReadRepository extends JpaRepository<OrderReadModel, String> {
    List<OrderReadModel> findByStatus(String status);
    List<OrderReadModel> findByProductId(Long productId);
}
