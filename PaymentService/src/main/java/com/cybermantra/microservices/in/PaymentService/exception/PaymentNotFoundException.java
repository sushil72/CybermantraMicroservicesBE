package com.cybermantra.microservices.in.PaymentService.exception;

import java.util.UUID;

public class PaymentNotFoundException extends RuntimeException {
    public PaymentNotFoundException(UUID id) {
        super("Payment not found with id: " + id);
    }
    public PaymentNotFoundException(String transactionId) {
        super("Payment not found with transaction id: " + transactionId);
    }
}
