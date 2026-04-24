package com.cybermantra.microservices.in.PaymentService.dto.response;

import com.cybermantra.microservices.in.PaymentService.enums.OrderStatus;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class OrderResponse {
    private UUID id;
    private String orderNumber;
    private UUID userId;
    private Long courseId;
    private BigDecimal amount;
    private String currency;
    private BigDecimal discountAmount;
    private BigDecimal finalAmount;
    private String couponCode;
    private OrderStatus status;
    private String paymentMethod;
    private LocalDateTime createdAt;
    private LocalDateTime completedAt;
}
