package com.ecommerce.orderservice.entity.read;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

/**
 * Read Model Entity (Query Model)
 * Denormalized, optimized for fast read operations
 * Kept in sync with Write DB via eventual consistency (OrderProjector)
 */
@Entity
@Table(name = "order_read_model")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderReadModel {
    @Id
    private String id;
    private Long productId;
    private String productName; // Denormalized from product
    private Double amount;
    private String status;
    private String customerName; // Denormalized from order metadata
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime projectedAt; // When this read model was last updated
}
