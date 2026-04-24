package com.cybermantra.microservices.in.PaymentService.exception;

import java.util.UUID;

public class RefundNotFoundException extends RuntimeException {
    public RefundNotFoundException(UUID id) {
        super("Refund not found with id: " + id);
    }
}
