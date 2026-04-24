package com.cybermantra.microservices.in.PaymentService.service;

import com.cybermantra.microservices.in.PaymentService.dto.request.CreateOrderRequest;
import com.cybermantra.microservices.in.PaymentService.dto.response.OrderResponse;
import com.cybermantra.microservices.in.PaymentService.entity.Coupon;
import com.cybermantra.microservices.in.PaymentService.entity.Order;
import com.cybermantra.microservices.in.PaymentService.enums.DiscountType;
import com.cybermantra.microservices.in.PaymentService.enums.OrderStatus;
import com.cybermantra.microservices.in.PaymentService.exception.AccessDeniedException;
import com.cybermantra.microservices.in.PaymentService.exception.DuplicateOrderException;
import com.cybermantra.microservices.in.PaymentService.exception.InvalidCouponException;
import com.cybermantra.microservices.in.PaymentService.exception.OrderNotFoundException;
import com.cybermantra.microservices.in.PaymentService.repository.CouponRepository;
import com.cybermantra.microservices.in.PaymentService.repository.OrderRepository;
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
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("OrderService Tests")
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private CouponRepository couponRepository;

    @InjectMocks
    private OrderService orderService;

    private UUID userId;
    private Long courseId;
    private Order mockOrder;

    private UUID orderId=null;

    @BeforeEach
    void setUp() {
        userId = UUID.fromString("1035beb8-307a-4379-bc2c-97157aa78bbf");
        orderId = UUID.randomUUID(); // ✅ UUID not Long
        courseId = 101L;
        mockOrder = Order.builder()
                .id(orderId)             // ✅ UUID
                .orderNumber("ORD-TEST-001")
                .userId(userId)
                .courseId(courseId)
                .amount(new BigDecimal("999.00"))
                .currency("USD")
                .discountAmount(BigDecimal.ZERO)
                .finalAmount(new BigDecimal("999.00"))
                .status(OrderStatus.PENDING)
                .build();
    }

    @Nested
    @DisplayName("Create Order Tests")
    class CreateOrderTests {

        @Test
        @DisplayName("Should create order successfully without coupon")
        void shouldCreateOrderSuccessfullyWithoutCoupon() {
            // Arrange
            CreateOrderRequest request = new CreateOrderRequest(courseId, null, "USD");
            when(orderRepository.existsByUserIdAndCourseIdAndStatus(userId, courseId, OrderStatus.COMPLETED))
                    .thenReturn(false);
            when(orderRepository.save(any(Order.class))).thenReturn(mockOrder);

            // Act
            OrderResponse response = orderService.createOrder(userId, request);

            // Assert
            assertThat(response).isNotNull();
            assertThat(response.getCourseId()).isEqualTo(courseId);
            assertThat(response.getUserId()).isEqualTo(userId);
            assertThat(response.getStatus()).isEqualTo(OrderStatus.PENDING);
            assertThat(response.getFinalAmount()).isEqualByComparingTo(new BigDecimal("999.00"));
            verify(orderRepository, times(1)).save(any(Order.class));
        }

        @Test
        @DisplayName("Should create order with PERCENTAGE coupon applied")
        void shouldCreateOrderWithPercentageCoupon() {
            // Arrange
            Coupon coupon = Coupon.builder()
                    .code("SAVE20")
                    .discountType(DiscountType.PERCENTAGE)
                    .discountValue(new BigDecimal("20"))
                    .currentUses(0).maxUses(100).isActive(true)
                    .validFrom(LocalDateTime.now().minusDays(1))
                    .validUntil(LocalDateTime.now().plusDays(30))
                    .build();

            CreateOrderRequest request = new CreateOrderRequest(courseId, "SAVE20", "USD");
            Order discountedOrder = Order.builder()
                    .id(UUID.randomUUID())
                    .orderNumber("ORD-TEST-002").userId(userId).courseId(courseId)
                    .amount(new BigDecimal("999.00")).currency("USD")
                    .discountAmount(new BigDecimal("199.80"))
                    .finalAmount(new BigDecimal("799.20"))
                    .couponCode("SAVE20").status(OrderStatus.PENDING).build();

            when(orderRepository.existsByUserIdAndCourseIdAndStatus(userId, courseId, OrderStatus.COMPLETED))
                    .thenReturn(false);
            when(couponRepository.findValidCoupon(eq("SAVE20"), any(LocalDateTime.class)))
                    .thenReturn(Optional.of(coupon));
            when(orderRepository.save(any(Order.class))).thenReturn(discountedOrder);

            // Act
            OrderResponse response = orderService.createOrder(userId, request);

            // Assert
            assertThat(response.getCouponCode()).isEqualTo("SAVE20");
            assertThat(response.getDiscountAmount()).isEqualByComparingTo(new BigDecimal("199.80"));
            assertThat(response.getFinalAmount()).isEqualByComparingTo(new BigDecimal("799.20"));
            verify(couponRepository).save(any(Coupon.class)); // coupon usage incremented
        }

        @Test
        @DisplayName("Should create order with FIXED coupon applied")
        void shouldCreateOrderWithFixedCoupon() {
            // Arrange
            Coupon coupon = Coupon.builder()
                    .code("FLAT100")
                    .discountType(DiscountType.FIXED)
                    .discountValue(new BigDecimal("100.00"))
                    .currentUses(0).maxUses(50).isActive(true)
                    .validFrom(LocalDateTime.now().minusDays(1))
                    .validUntil(LocalDateTime.now().plusDays(30))
                    .build();

            CreateOrderRequest request = new CreateOrderRequest(courseId, "FLAT100", "USD");
            Order discountedOrder = Order.builder()
                    .orderNumber("ORD-TEST-003").userId(userId).courseId(courseId)
                    .amount(new BigDecimal("999.00")).currency("USD")
                    .discountAmount(new BigDecimal("100.00"))
                    .finalAmount(new BigDecimal("899.00"))
                    .couponCode("FLAT100").status(OrderStatus.PENDING).build();

            when(orderRepository.existsByUserIdAndCourseIdAndStatus(userId, courseId, OrderStatus.COMPLETED))
                    .thenReturn(false);
            when(couponRepository.findValidCoupon(eq("FLAT100"), any(LocalDateTime.class)))
                    .thenReturn(Optional.of(coupon));
            when(orderRepository.save(any(Order.class))).thenReturn(discountedOrder);

            // Act
            OrderResponse response = orderService.createOrder(userId, request);

            // Assert
            assertThat(response.getDiscountAmount()).isEqualByComparingTo(new BigDecimal("100.00"));
            assertThat(response.getFinalAmount()).isEqualByComparingTo(new BigDecimal("899.00"));
        }

        @Test
        @DisplayName("Should throw DuplicateOrderException when user already completed order")
        void shouldThrowDuplicateOrderExceptionWhenAlreadyCompleted() {
            // Arrange
            CreateOrderRequest request = new CreateOrderRequest(courseId, null, "USD");
            when(orderRepository.existsByUserIdAndCourseIdAndStatus(userId, courseId, OrderStatus.COMPLETED))
                    .thenReturn(true);

            // Act & Assert
            assertThatThrownBy(() -> orderService.createOrder(userId, request))
                    .isInstanceOf(DuplicateOrderException.class)
                    .hasMessageContaining("already has a completed order for course");

            verify(orderRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should throw InvalidCouponException when coupon is invalid")
        void shouldThrowInvalidCouponExceptionWhenCouponInvalid() {
            // Arrange
            CreateOrderRequest request = new CreateOrderRequest(courseId, "FAKECODE", "USD");
            when(orderRepository.existsByUserIdAndCourseIdAndStatus(userId, courseId, OrderStatus.COMPLETED))
                    .thenReturn(false);
            when(couponRepository.findValidCoupon(eq("FAKECODE"), any(LocalDateTime.class)))
                    .thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> orderService.createOrder(userId, request))
                    .isInstanceOf(InvalidCouponException.class)
                    .hasMessageContaining("invalid or expired");
        }
    }

    @Nested
    @DisplayName("Get Order Tests")
    class GetOrderTests {

        // ─── GetOrderTests ────────────────────────────────────────────────────────────
        @Test
        @DisplayName("Should return order when found and user is owner")
        void shouldReturnOrderWhenFoundAndUserIsOwner() {
            when(orderRepository.findById(orderId)).thenReturn(Optional.of(mockOrder)); // ✅ UUID

            OrderResponse response = orderService.getOrder(orderId, userId); // ✅ UUID

            assertThat(response).isNotNull();
            assertThat(response.getUserId()).isEqualTo(userId);
        }

        @Test
        @DisplayName("Should throw OrderNotFoundException when order not found")
        void shouldThrowOrderNotFoundExceptionWhenOrderNotFound() {
            UUID unknownId = UUID.randomUUID(); // ✅ UUID
            when(orderRepository.findById(unknownId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> orderService.getOrder(unknownId, userId))
                    .isInstanceOf(OrderNotFoundException.class);
        }

        @Test
        @DisplayName("Should throw AccessDeniedException when user is not owner")
        void shouldThrowAccessDeniedExceptionWhenUserIsNotOwner() {
            UUID differentUser = UUID.randomUUID();
            when(orderRepository.findById(orderId)).thenReturn(Optional.of(mockOrder)); // ✅ UUID

            assertThatThrownBy(() -> orderService.getOrder(orderId, differentUser)) // ✅ UUID
                    .isInstanceOf(AccessDeniedException.class);
        }

        @Test
        @DisplayName("Should return all orders for user")
        void shouldReturnAllOrdersForUser() {
            Order order2 = Order.builder()
                    .id(UUID.randomUUID()) // ✅ UUID
                    .userId(userId).courseId(202L)
                    .amount(new BigDecimal("999.00")).finalAmount(new BigDecimal("999.00"))
                    .status(OrderStatus.COMPLETED).build();
            when(orderRepository.findAllByUserId(userId)).thenReturn(List.of(mockOrder, order2));

            List<OrderResponse> responses = orderService.getUserOrders(userId);

            assertThat(responses).hasSize(2);
            assertThat(responses).extracting(OrderResponse::getUserId).containsOnly(userId);
        }
    }

    @Nested
    @DisplayName("Order Status Tests")
    class OrderStatusTests {

        // ─── OrderStatusTests ─────────────────────────────────────────────────────────
        @Test
        @DisplayName("Should mark order as COMPLETED")
        void shouldMarkOrderAsCompleted() {
            when(orderRepository.findById(orderId)).thenReturn(Optional.of(mockOrder)); // ✅ UUID
            when(orderRepository.save(any(Order.class))).thenReturn(mockOrder);

            orderService.markOrderCompleted(orderId); // ✅ UUID

            verify(orderRepository).save(argThat(order ->
                    order.getStatus() == OrderStatus.COMPLETED &&
                            order.getCompletedAt() != null));
        }

        @Test
        @DisplayName("Should mark order as FAILED")
        void shouldMarkOrderAsFailed() {
            when(orderRepository.findById(orderId)).thenReturn(Optional.of(mockOrder)); // ✅ UUID
            when(orderRepository.save(any(Order.class))).thenReturn(mockOrder);

            orderService.markOrderFailed(orderId); // ✅ UUID

            verify(orderRepository).save(argThat(order -> order.getStatus() == OrderStatus.FAILED));
        }

        @Test
        @DisplayName("Should mark order as REFUNDED")
        void shouldMarkOrderAsRefunded() {
            when(orderRepository.findById(orderId)).thenReturn(Optional.of(mockOrder)); // ✅ UUID
            when(orderRepository.save(any(Order.class))).thenReturn(mockOrder);

            orderService.markOrderRefunded(orderId); // ✅ UUID

            verify(orderRepository).save(argThat(order -> order.getStatus() == OrderStatus.REFUNDED));
        }
}
}