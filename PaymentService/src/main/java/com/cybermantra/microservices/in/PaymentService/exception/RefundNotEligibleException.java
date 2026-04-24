package com.cybermantra.microservices.in.PaymentService.exception;

public class RefundNotEligibleException extends RuntimeException {
    public RefundNotEligibleException(String message) {
        super(message);
    }
}