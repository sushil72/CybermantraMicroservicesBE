package com.cybermantra.microservices.in.PaymentService.entity;

import com.cybermantra.microservices.in.PaymentService.enums.PaymentGateway;
import com.cybermantra.microservices.in.PaymentService.enums.PaymentStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "payments")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Payment {

    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_gateway", length = 50)
    private PaymentGateway paymentGateway;

    @Column(name = "transaction_id", unique = true, length = 255)
    private String transactionId;

    @Column(name = "amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Column(name = "currency", length = 3)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 50)
    @Builder.Default
    private PaymentStatus status = PaymentStatus.PENDING;

    // Stores card type, last4, upi id etc. as JSONB
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "payment_method_details", columnDefinition = "jsonb")
    private Map<String, Object> paymentMethodDetails;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
