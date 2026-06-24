package com.ecommerce.orderservice.service;

import com.ecommerce.orderservice.client.NotificationClient;
import com.ecommerce.orderservice.client.NotificationEvent;
import com.ecommerce.orderservice.entity.OutboxEvent;
import com.ecommerce.orderservice.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class OutboxPoller {
    private final OutboxEventRepository outboxEventRepository;
    private final NotificationClient notificationClient;

    @Scheduled(fixedRate = 5000)
    public void pollAndSend() {
        List<OutboxEvent> pendingEvents = outboxEventRepository.findByProcessedFalse();
        for (OutboxEvent event : pendingEvents) {
            try {
                notificationClient.sendEvent(new NotificationEvent(event.getId(), event.getEventType(), event.getPayload()));
                event.setProcessed(true);
                outboxEventRepository.save(event);
                System.out.println("Outbox Event Sent: " + event.getId());
            } catch (Exception e) {
                System.out.println("Failed to send Outbox Event " + event.getId() + ". Will retry.");
            }
        }
    }
}
