package com.ecommerce.orderservice.repository.write;

import com.ecommerce.orderservice.entity.write.OutboxEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface OutboxEventWriteRepository extends JpaRepository<OutboxEvent, String> {
    @Query("SELECT e FROM OutboxEvent e WHERE e.published = false")
    List<OutboxEvent> findUnpublishedEvents();
}
