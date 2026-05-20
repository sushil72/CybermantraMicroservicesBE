package com.cybermantra.microservices.in.PaymentService.events;

import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class PaymentFailedEvents {
    private String eventId;
    private String eventType;
    private String sourceService;
    private LocalDateTime timestamp;
    private UUID orderId;
    private UUID userId;
    private Long courseId;
    private String reason;
    private String paymentGateway;
}
