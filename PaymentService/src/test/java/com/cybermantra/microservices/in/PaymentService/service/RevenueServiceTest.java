package com.cybermantra.microservices.in.PaymentService.service;

import com.cybermantra.microservices.in.PaymentService.dto.response.RevenueResponse;
import com.cybermantra.microservices.in.PaymentService.repository.InstructorPayoutRepository;
import com.cybermantra.microservices.in.PaymentService.repository.OrderRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RevenueService Tests")
class RevenueServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private InstructorPayoutRepository payoutRepository;

    @InjectMocks
    private RevenueService revenueService;

    private final UUID instructorId = UUID.randomUUID();

    @Test
    @DisplayName("Should calculate revenue with 70/30 split correctly")
    void shouldCalculateRevenueSplitCorrectly() {
        // Arrange
        LocalDate from = LocalDate.now().withDayOfMonth(1);
        LocalDate to = LocalDate.now();
        when(payoutRepository.sumProcessedPayouts(eq(instructorId), any(), any()))
                .thenReturn(new BigDecimal("1000.00"));

        // Act
        RevenueResponse response = revenueService.getRevenue(instructorId, from, to);

        // Assert
        assertThat(response.getTotalRevenue()).isEqualByComparingTo(new BigDecimal("1000.00"));
        assertThat(response.getInstructorEarnings()).isEqualByComparingTo(new BigDecimal("700.00")); // 70%
        assertThat(response.getPlatformCommission()).isEqualByComparingTo(new BigDecimal("300.00")); // 30%
        assertThat(response.getCurrency()).isEqualTo("USD");
    }

    @Test
    @DisplayName("Should return zero revenue when no payouts exist")
    void shouldReturnZeroRevenueWhenNoPayouts() {
        // Arrange
        when(payoutRepository.sumProcessedPayouts(eq(instructorId), any(), any())).thenReturn(null);

        // Act
        RevenueResponse response = revenueService.getRevenue(instructorId, null, null);

        // Assert
        assertThat(response.getTotalRevenue()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(response.getInstructorEarnings()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(response.getPlatformCommission()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("Should default to current month when dates are null")
    void shouldDefaultToCurrentMonthWhenDatesAreNull() {
        // Arrange
        when(payoutRepository.sumProcessedPayouts(eq(instructorId), any(), any()))
                .thenReturn(BigDecimal.ZERO);

        // Act
        RevenueResponse response = revenueService.getRevenue(instructorId, null, null);

        // Assert
        assertThat(response.getPeriodStart()).isEqualTo(LocalDate.now().withDayOfMonth(1));
        assertThat(response.getPeriodEnd()).isEqualTo(LocalDate.now());
    }

    @Test
    @DisplayName("Should use provided date range when specified")
    void shouldUseDateRangeWhenSpecified() {
        // Arrange
        LocalDate from = LocalDate.of(2026, 1, 1);
        LocalDate to = LocalDate.of(2026, 3, 31);
        when(payoutRepository.sumProcessedPayouts(eq(instructorId), eq(from), eq(to)))
                .thenReturn(new BigDecimal("5000.00"));

        // Act
        RevenueResponse response = revenueService.getRevenue(instructorId, from, to);

        // Assert
        assertThat(response.getPeriodStart()).isEqualTo(from);
        assertThat(response.getPeriodEnd()).isEqualTo(to);
        assertThat(response.getTotalRevenue()).isEqualByComparingTo(new BigDecimal("5000.00"));
    }
}