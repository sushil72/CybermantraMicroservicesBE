package com.cybermantra.microservices.in.PaymentService.dto.response;
import lombok.*;
import java.util.UUID;
import java.math.BigDecimal;
import java.time.LocalDate;
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class RevenueResponse {
    private UUID instructorId;
    private BigDecimal totalRevenue;
    private BigDecimal platformCommission;
    private BigDecimal instructorEarnings;
    private Long totalOrders;
    private String currency;
    private LocalDate periodStart;
    private LocalDate periodEnd;
}
