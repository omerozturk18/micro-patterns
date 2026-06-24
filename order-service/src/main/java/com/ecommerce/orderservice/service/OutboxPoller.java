package com.ecommerce.orderservice.service;

import com.ecommerce.orderservice.client.NotificationClient;
import com.ecommerce.orderservice.client.NotificationEvent;
import com.ecommerce.orderservice.entity.write.OutboxEvent;
import com.ecommerce.orderservice.repository.write.OutboxEventWriteRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * OutboxPoller
 * Implements the Outbox Pattern for reliable event publishing
 * Polls unpublished events from Write DB and sends them to external services
 * Ensures no event is lost even if the service crashes
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OutboxPoller {
    private final OutboxEventWriteRepository outboxEventWriteRepository;
    private final NotificationClient notificationClient;

    /**
     * Polls unpublished events from the Outbox table every 5 seconds
     * and attempts to send them to the Notification service
     */
    @Scheduled(fixedRate = 5000)
    @Transactional(transactionManager = "writeTransactionManager")
    public void pollAndSend() {
        log.debug("Polling for unpublished events...");
        List<OutboxEvent> pendingEvents = outboxEventWriteRepository.findUnpublishedEvents();
        
        for (OutboxEvent event : pendingEvents) {
            try {
                notificationClient.sendEvent(
                    new NotificationEvent(event.getId(), event.getEventType(), event.getPayload())
                );
                event.setPublished(true);
                outboxEventWriteRepository.save(event);
                log.info("Outbox Event Sent: {}", event.getId());
            } catch (Exception e) {
                log.warn("Failed to send Outbox Event {}. Will retry.", event.getId(), e);
            }
        }
    }
}
