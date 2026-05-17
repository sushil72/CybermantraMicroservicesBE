package com.cybermantra.microservices.in.PaymentService.controller;

import com.cybermantra.microservices.in.PaymentService.dto.request.ProcessPaymentRequest;
import com.cybermantra.microservices.in.PaymentService.dto.response.ApiResponse;
import com.cybermantra.microservices.in.PaymentService.dto.response.PaymentResponse;
import com.cybermantra.microservices.in.PaymentService.service.PaymentService;
import com.cybermantra.microservices.in.PaymentService.service.WebhookService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("${api.baseurl}")
@RequiredArgsConstructor
@Tag(name = "Payments", description = "Payment processing endpoints")
public class PaymentController {

    private final PaymentService paymentservice;
    private final WebhookService webhookService;

    @PostMapping("/process")
    @Operation(summary = "Process payment", description = "Processes payment via Stripe or Razorpay for a given order.")
    public ResponseEntity<ApiResponse<PaymentResponse>> processPayment(
            @Valid @RequestBody ProcessPaymentRequest request) {
        PaymentResponse paymentResponse = paymentservice.processPayment(request);
        return ResponseEntity.ok(ApiResponse.success(paymentResponse));
    }

    @PostMapping("/webhook")
    @Operation(summary = "Payment gateway webhook",
            description = "Receives callbacks from Stripe and Razorpay. Do not call manually.")
    public ResponseEntity<String> handleWebhook(
            @RequestBody String payload,
            @RequestHeader(value = "Stripe-Signature", required = false) String stripeSignature,
            @RequestHeader(value = "X-Razorpay-Signature", required = false) String razorpaySignature) {

        if (stripeSignature != null) {
            // Request came from Stripe
            webhookService.handleStripeWebhook(payload, stripeSignature);
            
        } else if (razorpaySignature != null) {
            // Request came from Razorpay
            webhookService.handleRazorpayWebhook(payload, razorpaySignature);

        } else {
            // Unknown source — reject it
            return ResponseEntity.badRequest().body("Unknown webhook source");
        }

        // Always return 200 quickly — gateways retry if they don't get 200
        return ResponseEntity.ok("Webhook received");
    }
}
