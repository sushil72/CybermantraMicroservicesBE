package com.cybermantra.microservices.in.PaymentService.service;

import com.cybermantra.microservices.in.PaymentService.exception.PaymentProcessingException;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.param.PaymentIntentCreateParams;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@Slf4j
public class StripeService {

    @Value("${payment.stripe.secret-key}")
    private String secretKey;
    @Getter
    @Value("${payment.stripe.webhook-secret}")
    private String webhookSecret;
    @PostConstruct
    public void init() {
        log.debug("Initializing Stripe with secret key: {}", secretKey);
        Stripe.apiKey = secretKey;
    }

    public String charge(String paymentToken, BigDecimal amount,
                         String currency, String orderNumber) {
        try {
            // Convert amount to smallest currency unit (cents for USD)
            long amountInSmallestUnit = amount
                    .multiply(BigDecimal.valueOf(100))
                    .longValue();

            PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                    .setAmount(amountInSmallestUnit)
                    .setCurrency(currency.toLowerCase())
                    .setPaymentMethod(paymentToken)
                    .setConfirm(true)
                    .setDescription("LMS Course Purchase - Order: " + orderNumber)
                    .setAutomaticPaymentMethods(
                            PaymentIntentCreateParams.AutomaticPaymentMethods.builder()
                                    .setEnabled(true)
                                    .setAllowRedirects(
                                            PaymentIntentCreateParams
                                                    .AutomaticPaymentMethods
                                                    .AllowRedirects.NEVER)
                                    .build())
                    .build();

            PaymentIntent paymentIntent = PaymentIntent.create(params);
            log.info("Stripe PaymentIntent created: {}", paymentIntent.getId());
            return paymentIntent.getId();

        } catch (StripeException ex) {
            log.error("Stripe charge failed: {}", ex.getMessage());
            throw new PaymentProcessingException(
                    "Stripe payment failed: " + ex.getMessage(), ex);
        }
    }

}