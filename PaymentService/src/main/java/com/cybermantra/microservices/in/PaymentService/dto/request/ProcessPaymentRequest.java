package com.cybermantra.microservices.in.PaymentService.dto.request;

import com.cybermantra.microservices.in.PaymentService.enums.PaymentGateway;
import jakarta.validation.constraints.*;
import lombok.*;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor @AllArgsConstructor
public class ProcessPaymentRequest {

    @NotNull(message = "Order ID is required")
    private UUID orderId;

    @NotNull(message = "Payment gateway is required")
    private PaymentGateway paymentGateway;

    @NotBlank(message = "Payment method is required")
    private String paymentMethod; // card, upi, wallet

    // Gateway-specific token (Stripe paymentMethodId / Razorpay paymentId)
    @NotBlank(message = "Payment token is required")
    private String paymentToken;

    private String razorpayOrderId;
    private String razorpaySignature;
}
