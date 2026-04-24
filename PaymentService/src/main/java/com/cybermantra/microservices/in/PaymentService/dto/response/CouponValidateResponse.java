package com.cybermantra.microservices.in.PaymentService.dto.response;
import com.cybermantra.microservices.in.PaymentService.enums.*;
import lombok.*;

import java.math.BigDecimal;

@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class CouponValidateResponse {
    private String code;
    private DiscountType discountType;
    private BigDecimal discountValue;
    private BigDecimal originalAmount;
    private BigDecimal discountAmount;
    private BigDecimal finalAmount;
    private boolean valid;
    private String message;
}