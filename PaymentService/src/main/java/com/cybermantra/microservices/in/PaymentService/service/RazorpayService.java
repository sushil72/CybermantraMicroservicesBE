package com.cybermantra.microservices.in.PaymentService.service;

import com.cybermantra.microservices.in.PaymentService.exception.PaymentProcessingException;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import com.razorpay.Utils;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@Slf4j
public class RazorpayService {

    @Value("${payment.razorpay.key-id}")
    private String keyId;

    @Value("${payment.razorpay.key-secret}")
    private String keySecret;

    public String charge(String paymentId, BigDecimal amount,
                         String currency, String orderNumber) {
        try {
            RazorpayClient client = new RazorpayClient(keyId, keySecret);

            // Convert amount to smallest currency unit (paise for INR)
            int amountInSmallestUnit = amount
                    .multiply(BigDecimal.valueOf(100))
                    .intValue();

            JSONObject paymentCapture = new JSONObject();
            paymentCapture.put("amount", amountInSmallestUnit);
            paymentCapture.put("currency", currency);

            // Capture the payment
            com.razorpay.Payment payment = client.payments.capture(
                    paymentId, paymentCapture);

            log.info("Razorpay payment captured: {}", payment.get("id").toString());
            return payment.get("id").toString();

        } catch (RazorpayException ex) {
            log.error("Razorpay charge failed: {}", ex.getMessage());
            throw new PaymentProcessingException(
                    "Razorpay payment failed: " + ex.getMessage(), ex);
        }
    }
    public void verifyPayment(String orderId, String paymentId, String signature) {
        try {
            JSONObject options = new JSONObject();
            options.put("razorpay_order_id", orderId);
            options.put("razorpay_payment_id", paymentId);
            options.put("razorpay_signature", signature);

            Utils.verifyPaymentSignature(options, keySecret);

            log.info("Razorpay signature verified successfully");

        } catch (Exception e) {
            throw new PaymentProcessingException("Invalid Razorpay signature", e);
        }
    }
}