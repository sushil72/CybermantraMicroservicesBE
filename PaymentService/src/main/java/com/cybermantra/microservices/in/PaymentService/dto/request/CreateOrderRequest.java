package com.cybermantra.microservices.in.PaymentService.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

// CreateOrderRequest.java
@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CreateOrderRequest {

    @NotNull(message = "Course ID is required")
    private Long courseId;

    private String couponCode;

    @NotBlank(message = "Currency is required")
    @Builder.Default
    private String currency = "USD";
}