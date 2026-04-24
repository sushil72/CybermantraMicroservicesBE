package com.cybermantra.microservices.in.PaymentService.service;

import com.cybermantra.microservices.in.PaymentService.enums.PaymentStatus;
import com.cybermantra.microservices.in.PaymentService.repository.PaymentRepository;
import com.razorpay.Utils;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.model.PaymentIntent;
import com.stripe.net.Webhook;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class WebhookService {

    private final PaymentRepository paymentRepository;
    private final OrderService orderService;
    private final StripeService stripeService;
    @Value("${payment.razorpay.key-secret}")
    private String keySecret;
    @Transactional
    public void handleStripeWebhook(String payload, String sigHeader) {
        Event event;
        try {
            event = Webhook.constructEvent(
                    payload, sigHeader, stripeService.getWebhookSecret());
        } catch (SignatureVerificationException ex) {
            log.error("Invalid Stripe webhook signature: {}", ex.getMessage());
            throw new RuntimeException("Invalid webhook signature");
        }

        log.info("Received Stripe webhook event: {}", event.getType());

        switch (event.getType()) {
            case "payment_intent.succeeded" -> {
                PaymentIntent intent = (PaymentIntent) event
                        .getDataObjectDeserializer()
                        .getObject()
                        .orElseThrow();
                handleStripePaymentSuccess(intent.getId());
            }
            case "payment_intent.payment_failed" -> {
                PaymentIntent intent = (PaymentIntent) event
                        .getDataObjectDeserializer()
                        .getObject()
                        .orElseThrow();
                handleStripePaymentFailure(intent.getId());
            }
            default -> log.info("Unhandled Stripe event type: {}", event.getType());
        }
    }

    private void handleStripePaymentSuccess(String transactionId) {
        paymentRepository.findByTransactionId(transactionId).ifPresent(payment -> {
            payment.setStatus(PaymentStatus.SUCCESS);
            paymentRepository.save(payment);
            orderService.markOrderCompleted(payment.getOrder().getId());
            log.info("Webhook: payment {} confirmed successful", transactionId);
            // TODO: Publish PaymentSuccessEvent to trigger Enrollment Service
        });
    }

    private void handleStripePaymentFailure(String transactionId) {
        paymentRepository.findByTransactionId(transactionId).ifPresent(payment -> {
            payment.setStatus(PaymentStatus.FAILED);
            paymentRepository.save(payment);
            orderService.markOrderFailed(payment.getOrder().getId());
            log.info("Webhook: payment {} failed", transactionId);
        });
    }

    @Transactional
    public void handleRazorpayWebhook(String payload, String signature) {
        // Verify signature
        if (!verifyRazorpaySignature(payload, signature)) {
            log.error("Invalid Razorpay webhook signature");
            throw new RuntimeException("Invalid webhook signature");
        }

        try {
            JSONObject event = new JSONObject(payload);
            String eventType = event.getString("event");
            log.info("Received Razorpay webhook event: {}", eventType);

            switch (eventType) {
                case "payment.captured" -> {
                    String paymentId = event
                            .getJSONObject("payload")
                            .getJSONObject("payment")
                            .getJSONObject("entity")
                            .getString("id");
                    handleRazorpayPaymentSuccess(paymentId);
                }
                case "payment.failed" -> {
                    String paymentId = event
                            .getJSONObject("payload")
                            .getJSONObject("payment")
                            .getJSONObject("entity")
                            .getString("id");
                    handleRazorpayPaymentFailure(paymentId);
                }
                default -> log.info("Unhandled Razorpay event type: {}", eventType);
            }

        } catch (Exception ex) {
            log.error("Error processing Razorpay webhook: {}", ex.getMessage());
            throw new RuntimeException("Webhook processing failed", ex);
        }
    }

    private boolean verifyRazorpaySignature(String payload, String signature) {
        try {
            String expectedSignature = Utils.getHash(payload, keySecret);
            return expectedSignature.equals(signature);
        } catch (Exception e) {
            return false;
        }
    }

    private void handleRazorpayPaymentSuccess(String paymentId) {
        paymentRepository.findByTransactionId(paymentId).ifPresent(payment -> {
            payment.setStatus(PaymentStatus.SUCCESS);
            paymentRepository.save(payment);
            orderService.markOrderCompleted(payment.getOrder().getId());
            log.info("Razorpay webhook: payment {} confirmed successful", paymentId);
            // TODO: Publish PaymentSuccessEvent → triggers Enrollment Service
        });
    }

    private void handleRazorpayPaymentFailure(String paymentId) {
        paymentRepository.findByTransactionId(paymentId).ifPresent(payment -> {
            payment.setStatus(PaymentStatus.FAILED);
            paymentRepository.save(payment);
            orderService.markOrderFailed(payment.getOrder().getId());
            log.info("Razorpay webhook: payment {} failed", paymentId);
        });
    }
}