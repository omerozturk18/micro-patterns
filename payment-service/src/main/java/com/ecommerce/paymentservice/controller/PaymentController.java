package com.ecommerce.paymentservice.controller;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    @PostMapping
    public PaymentResponse processPayment(@RequestBody PaymentRequest request) {
        // Simulate payment: fails if amount > 1000
        if (request.amount() > 1000) {
            return new PaymentResponse(request.orderId(), "FAILED", "Insufficient Funds");
        }
        return new PaymentResponse(request.orderId(), "SUCCESS", "Payment Processed");
    }
}

record PaymentRequest(String orderId, Double amount) {}
record PaymentResponse(String orderId, String status, String message) {}
