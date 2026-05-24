package com.cybermantra.microservices.in.EnrollmentService.events;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class RefundProcessedEvent {
    private String eventId;
    private String eventType;
    private String sourceService;
    private LocalDateTime timestamp;
    private UUID refundId;
    private UUID orderId;
    private UUID userId;
    private Long courseId;
    private BigDecimal refundAmount;
}