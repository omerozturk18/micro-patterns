package com.ecommerce.orderservice.entity.write;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

/**
 * Write Model Entity (Command Model)
 * Normalized, single-responsibility structure for command processing
 */
@Entity
@Table(name = "orders")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Order {
    @Id
    private String id;
    private Long productId;
    private Double amount;
    private String status; // PENDING, COMPLETED, CANCELLED
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Order(String id, Long productId, Double amount, String status) {
        this.id = id;
        this.productId = productId;
        this.amount = amount;
        this.status = status;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }
}
