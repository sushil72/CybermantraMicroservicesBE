package com.cybermantra.microservices.in.PaymentService.dto.response;
import com.cybermantra.microservices.in.PaymentService.enums.PaymentGateway;
import com.cybermantra.microservices.in.PaymentService.enums.PaymentStatus;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class PaymentResponse {
    private UUID id;
    private UUID orderId;
    private PaymentGateway paymentGateway;
    private String transactionId;
    private BigDecimal amount;
    private String currency;
    private PaymentStatus status;
    private String errorMessage;
    private LocalDateTime createdAt;
}
