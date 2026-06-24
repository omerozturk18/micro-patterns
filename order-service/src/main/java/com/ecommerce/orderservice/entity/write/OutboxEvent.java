package com.ecommerce.orderservice.entity.write;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

/**
 * Outbox Pattern Entity for Write DB
 * Ensures reliable event publishing through Transactional Outbox Pattern
 */
@Entity
@Table(name = "outbox_events")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OutboxEvent {
    @Id
    private String id;
    private String eventType;
    private String payload;
    private boolean published;
    private LocalDateTime createdAt;

    public OutboxEvent(String id, String eventType, String payload, boolean published) {
        this.id = id;
        this.eventType = eventType;
        this.payload = payload;
        this.published = published;
        this.createdAt = LocalDateTime.now();
    }
}
