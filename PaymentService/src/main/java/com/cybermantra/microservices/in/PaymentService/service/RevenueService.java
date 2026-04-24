package com.cybermantra.microservices.in.PaymentService.service;

import com.cybermantra.microservices.in.PaymentService.dto.response.RevenueResponse;
import com.cybermantra.microservices.in.PaymentService.repository.InstructorPayoutRepository;
import com.cybermantra.microservices.in.PaymentService.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class RevenueService {

    private final OrderRepository orderRepository;
    private final InstructorPayoutRepository payoutRepository;

    private static final BigDecimal INSTRUCTOR_SHARE = new BigDecimal("0.70");
    private static final BigDecimal PLATFORM_SHARE = new BigDecimal("0.30");

    @Transactional(readOnly = true)
    public RevenueResponse getRevenue(UUID instructorId,
                                      LocalDate from, LocalDate to) {
        // Default to current month if no dates provided
        LocalDate periodStart = from != null ? from : LocalDate.now().withDayOfMonth(1);
        LocalDate periodEnd = to != null ? to : LocalDate.now();

        BigDecimal totalRevenue = payoutRepository.sumProcessedPayouts(
                instructorId, periodStart, periodEnd);

        if (totalRevenue == null) {
            totalRevenue = BigDecimal.ZERO;
        }

        BigDecimal instructorEarnings = totalRevenue
                .multiply(INSTRUCTOR_SHARE)
                .setScale(2, RoundingMode.HALF_UP);

        BigDecimal platformCommission = totalRevenue
                .multiply(PLATFORM_SHARE)
                .setScale(2, RoundingMode.HALF_UP);

        // Total completed orders in period
        BigDecimal platformTotal = orderRepository.sumCompletedOrdersBetween(
                periodStart.atStartOfDay(),
                periodEnd.atTime(23, 59, 59));

        log.info("Revenue report for instructor {} from {} to {}",
                instructorId, periodStart, periodEnd);

        return RevenueResponse.builder()
                .instructorId(instructorId)
                .totalRevenue(totalRevenue)
                .instructorEarnings(instructorEarnings)
                .platformCommission(platformCommission)
                .totalOrders(0L) // TODO: add count query
                .currency("USD")
                .periodStart(periodStart)
                .periodEnd(periodEnd)
                .build();
    }
}