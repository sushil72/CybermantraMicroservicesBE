package com.cybermantra.microservices.in.PaymentService.exception;

import java.util.UUID;

public class DuplicateOrderException extends RuntimeException {
    public DuplicateOrderException(UUID userId, Long courseId) {
        super("User already has a completed order for course: " + courseId);
    }
}
