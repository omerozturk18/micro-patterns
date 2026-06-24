package com.ecommerce.notificationservice.repository;

import com.ecommerce.notificationservice.entity.InboxEvent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InboxEventRepository extends JpaRepository<InboxEvent, String> {
}
