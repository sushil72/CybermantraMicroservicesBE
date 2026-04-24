package com.cybermantra.microservices.in.PaymentService.dto.response;
import com.cybermantra.microservices.in.PaymentService.enums.RefundStatus;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class RefundResponse {
    private UUID id;
    private UUID orderId;
    private BigDecimal refundAmount;
    private String reason;
    private RefundStatus status;
    private UUID requestedBy;
    private LocalDateTime requestedAt;
    private LocalDateTime processedAt;
}
