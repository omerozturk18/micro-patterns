package com.ecommerce.notificationservice.controller;

import com.ecommerce.notificationservice.entity.InboxEvent;
import com.ecommerce.notificationservice.repository.InboxEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {
    private final InboxEventRepository inboxEventRepository;

    @PostMapping("/inbox")
    @Transactional
    public String handleEvent(@RequestBody EventDto event) {
        if (inboxEventRepository.existsById(event.id())) {
            System.out.println("Idempotency Check: Event " + event.id() + " already processed. Skipping.");
            return "Already processed";
        }

        System.out.println("Processing Notification for Event: " + event.eventType() + " Payload: " + event.payload());
        
        // Inbox Pattern: Save to ensure it won't be processed again
        inboxEventRepository.save(new InboxEvent(event.id(), LocalDateTime.now()));
        
        return "Processed successfully";
    }
}

record EventDto(String id, String eventType, String payload) {}
