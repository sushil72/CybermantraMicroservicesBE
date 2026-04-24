package com.cybermantra.microservices.in.PaymentService.exception;

public class CouponNotFoundException extends RuntimeException {
    public CouponNotFoundException(String code) {
        super("Coupon not found or invalid: " + code);
    }
}