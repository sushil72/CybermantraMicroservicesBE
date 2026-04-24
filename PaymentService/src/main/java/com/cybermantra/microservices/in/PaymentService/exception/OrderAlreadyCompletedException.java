package com.cybermantra.microservices.in.PaymentService.exception;

import java.util.UUID;

public class OrderAlreadyCompletedException extends RuntimeException {
    public OrderAlreadyCompletedException(UUID orderId) {
        super("Order " + orderId + " is already completed");
    }
}
