package com.cybermantra.microservices.in.PaymentService.service;

import com.cybermantra.microservices.in.PaymentService.dto.request.RefundRequest;
import com.cybermantra.microservices.in.PaymentService.dto.response.RefundResponse;
import com.cybermantra.microservices.in.PaymentService.entity.Order;
import com.cybermantra.microservices.in.PaymentService.entity.Refund;
import com.cybermantra.microservices.in.PaymentService.enums.OrderStatus;
import com.cybermantra.microservices.in.PaymentService.enums.RefundStatus;
import com.cybermantra.microservices.in.PaymentService.exception.AccessDeniedException;
import com.cybermantra.microservices.in.PaymentService.exception.RefundNotEligibleException;
import com.cybermantra.microservices.in.PaymentService.exception.RefundNotFoundException;
import com.cybermantra.microservices.in.PaymentService.repository.RefundRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.any;

@ExtendWith(MockitoExtension.class)
@DisplayName("RefundService Tests")
class RefundServiceTest {

    @Mock private RefundRepository refundRepository;
    @Mock private OrderService orderService;
    @InjectMocks private RefundService refundService;

    private UUID userId;
    private UUID orderId;
    private UUID refundId;
    private Order completedOrder;

    @BeforeEach
    void setUp() {
        userId   = UUID.fromString("1035beb8-307a-4379-bc2c-97157aa78bbf");
        orderId  = UUID.randomUUID();
        refundId = UUID.randomUUID();

        completedOrder = Order.builder()
                .id(orderId)
                .orderNumber("ORD-TEST-001")
                .userId(userId)
                .courseId(101L)
                .amount(new BigDecimal("999.00"))
                .finalAmount(new BigDecimal("999.00"))
                .status(OrderStatus.COMPLETED)
                .completedAt(LocalDateTime.now().minusDays(5))
                .build();
    }

    @Nested
    @DisplayName("Request Refund Tests")
    class RequestRefundTests {

        @Test
        @DisplayName("Should submit refund request successfully")
        void shouldSubmitRefundRequestSuccessfully() {
            // Arrange
            RefundRequest request = new RefundRequest(orderId, new BigDecimal("999.00"), "Course not as described");
            Refund savedRefund = Refund.builder()
                    .id(refundId)
                    .order(completedOrder)
                    .refundAmount(new BigDecimal("999.00"))
                    .reason("Course not as described")
                    .requestedBy(userId)
                    .status(RefundStatus.PENDING)
                    .requestedAt(LocalDateTime.now())
                    .build();

            when(orderService.findOrderById(orderId)).thenReturn(completedOrder);
            when(refundRepository.existsByOrderIdAndStatusNot(orderId, RefundStatus.REJECTED)).thenReturn(false);
            when(refundRepository.save(any(Refund.class))).thenReturn(savedRefund);

            // Act
            RefundResponse response = refundService.requestRefund(userId, request);

            // Assert
            assertThat(response).isNotNull();
            assertThat(response.getStatus()).isEqualTo(RefundStatus.PENDING);
            assertThat(response.getRefundAmount()).isEqualByComparingTo(new BigDecimal("999.00"));
            assertThat(response.getRequestedBy()).isEqualTo(userId);
        }

        @Test
        @DisplayName("Should submit partial refund successfully")
        void shouldSubmitPartialRefundSuccessfully() {
            // Arrange
            RefundRequest request = new RefundRequest(orderId, new BigDecimal("500.00"), "Partial refund request");
            Refund savedRefund = Refund.builder()
                    .id(refundId)
                    .order(completedOrder)
                    .refundAmount(new BigDecimal("500.00"))
                    .reason("Partial refund request")
                    .requestedBy(userId)
                    .status(RefundStatus.PENDING)
                    .build();

            when(orderService.findOrderById(orderId)).thenReturn(completedOrder);
            when(refundRepository.existsByOrderIdAndStatusNot(orderId, RefundStatus.REJECTED)).thenReturn(false);
            when(refundRepository.save(any(Refund.class))).thenReturn(savedRefund);

            // Act
            RefundResponse response = refundService.requestRefund(userId, request);

            // Assert
            assertThat(response.getRefundAmount()).isEqualByComparingTo(new BigDecimal("500.00"));
        }

        @Test
        @DisplayName("Should throw AccessDeniedException when user is not order owner")
        void shouldThrowAccessDeniedWhenUserIsNotOwner() {
            // Arrange
            UUID differentUser = UUID.randomUUID();
            RefundRequest request = new RefundRequest(orderId, new BigDecimal("999.00"), "Want refund");
            when(orderService.findOrderById(orderId)).thenReturn(completedOrder);

            // Act & Assert
            assertThatThrownBy(() -> refundService.requestRefund(differentUser, request))
                    .isInstanceOf(AccessDeniedException.class);
        }

        @Test
        @DisplayName("Should throw RefundNotEligibleException for non-completed order")
        void shouldThrowExceptionForNonCompletedOrder() {
            // Arrange
            UUID pendingOrderId = UUID.randomUUID();
            Order pendingOrder = Order.builder()
                    .id(pendingOrderId)
                    .userId(userId)
                    .courseId(101L)
                    .finalAmount(new BigDecimal("999.00"))
                    .status(OrderStatus.PENDING)
                    .build();
            RefundRequest request = new RefundRequest(pendingOrderId, new BigDecimal("999.00"), "Cancel order");
            when(orderService.findOrderById(pendingOrderId)).thenReturn(pendingOrder);

            // Act & Assert
            assertThatThrownBy(() -> refundService.requestRefund(userId, request))
                    .isInstanceOf(RefundNotEligibleException.class)
                    .hasMessageContaining("Only completed orders are eligible");
        }

        @Test
        @DisplayName("Should throw RefundNotEligibleException when outside 30-day window")
        void shouldThrowExceptionWhenOutside30DayWindow() {
            // Arrange
            UUID oldOrderId = UUID.randomUUID();
            Order oldOrder = Order.builder()
                    .id(oldOrderId)
                    .userId(userId)
                    .courseId(101L)
                    .finalAmount(new BigDecimal("999.00"))
                    .status(OrderStatus.COMPLETED)
                    .completedAt(LocalDateTime.now().minusDays(31))
                    .build();
            RefundRequest request = new RefundRequest(oldOrderId, new BigDecimal("999.00"), "Late refund");
            when(orderService.findOrderById(oldOrderId)).thenReturn(oldOrder);

            // Act & Assert
            assertThatThrownBy(() -> refundService.requestRefund(userId, request))
                    .isInstanceOf(RefundNotEligibleException.class)
                    .hasMessageContaining("30 days");
        }

        @Test
        @DisplayName("Should throw RefundNotEligibleException for duplicate refund request")
        void shouldThrowExceptionForDuplicateRefundRequest() {
            // Arrange
            RefundRequest request = new RefundRequest(orderId, new BigDecimal("999.00"), "Duplicate");
            when(orderService.findOrderById(orderId)).thenReturn(completedOrder);
            when(refundRepository.existsByOrderIdAndStatusNot(orderId, RefundStatus.REJECTED)).thenReturn(true);

            // Act & Assert
            assertThatThrownBy(() -> refundService.requestRefund(userId, request))
                    .isInstanceOf(RefundNotEligibleException.class)
                    .hasMessageContaining("refund request already exists");
        }

        @Test
        @DisplayName("Should throw RefundNotEligibleException when refund exceeds order amount")
        void shouldThrowExceptionWhenRefundExceedsOrderAmount() {
            // Arrange
            RefundRequest request = new RefundRequest(orderId, new BigDecimal("9999.00"), "Too much");
            when(orderService.findOrderById(orderId)).thenReturn(completedOrder);
            when(refundRepository.existsByOrderIdAndStatusNot(orderId, RefundStatus.REJECTED)).thenReturn(false);

            // Act & Assert
            assertThatThrownBy(() -> refundService.requestRefund(userId, request))
                    .isInstanceOf(RefundNotEligibleException.class)
                    .hasMessageContaining("cannot exceed order amount");
        }
    }

    @Nested
    @DisplayName("Get Refund Tests")
    class GetRefundTests {

        @Test
        @DisplayName("Should return refund when user is requester")
        void shouldReturnRefundWhenUserIsRequester() {
            // Arrange
            Refund refund = Refund.builder()
                    .id(refundId)
                    .order(completedOrder)
                    .refundAmount(new BigDecimal("999.00"))
                    .requestedBy(userId)
                    .status(RefundStatus.PENDING)
                    .build();
            when(refundRepository.findById(refundId)).thenReturn(Optional.of(refund));

            // Act
            RefundResponse response = refundService.getRefund(refundId, userId);

            // Assert
            assertThat(response).isNotNull();
            assertThat(response.getRequestedBy()).isEqualTo(userId);
        }

        @Test
        @DisplayName("Should throw RefundNotFoundException for non-existent refund")
        void shouldThrowRefundNotFoundExceptionForNonExistentRefund() {
            // Arrange
            UUID unknownId = UUID.randomUUID();
            when(refundRepository.findById(unknownId)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> refundService.getRefund(unknownId, userId))
                    .isInstanceOf(RefundNotFoundException.class);
        }

        @Test
        @DisplayName("Should throw AccessDeniedException when user is not requester")
        void shouldThrowAccessDeniedWhenUserIsNotRequester() {
            // Arrange
            UUID differentUser = UUID.randomUUID();
            Refund refund = Refund.builder()
                    .id(refundId)
                    .order(completedOrder)
                    .requestedBy(userId)
                    .status(RefundStatus.PENDING)
                    .build();
            when(refundRepository.findById(refundId)).thenReturn(Optional.of(refund));

            // Act & Assert
            assertThatThrownBy(() -> refundService.getRefund(refundId, differentUser))
                    .isInstanceOf(AccessDeniedException.class);
        }
    }
}