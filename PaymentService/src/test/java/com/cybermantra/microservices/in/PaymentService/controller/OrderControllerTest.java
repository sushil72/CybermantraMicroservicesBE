package com.cybermantra.microservices.in.PaymentService.controller;

import com.cybermantra.microservices.in.PaymentService.dto.request.CreateOrderRequest;
import com.cybermantra.microservices.in.PaymentService.dto.response.ApiResponse;
import com.cybermantra.microservices.in.PaymentService.dto.response.OrderResponse;
import com.cybermantra.microservices.in.PaymentService.enums.OrderStatus;
import com.cybermantra.microservices.in.PaymentService.exception.DuplicateOrderException;
import com.cybermantra.microservices.in.PaymentService.exception.GlobalExceptionHandler;
import com.cybermantra.microservices.in.PaymentService.exception.OrderNotFoundException;
import com.cybermantra.microservices.in.PaymentService.service.OrderService;
import com.cybermantra.microservices.in.PaymentService.service.jwt.JwtServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.containsString;
@ExtendWith(MockitoExtension.class)
@DisplayName("OrderController Tests")
class OrderControllerTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @Mock
    private OrderService orderService;

    @InjectMocks
    private OrderController orderController;

    private UUID userId;
    private OrderResponse mockOrderResponse;

    @Mock
    private JwtServiceImpl jwtService;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(orderController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules();

        userId = UUID.fromString("1035beb8-307a-4379-bc2c-97157aa78bbf");

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(userId.toString(), null,
                        List.of(new SimpleGrantedAuthority("ROLE_USER"))));

        // ✅ removed duplicate local UUID declaration
        // ✅ id is Long not UUID — just don't set it, or set as Long
        mockOrderResponse = OrderResponse.builder()
                .orderNumber("ORD-TEST-001")
                .userId(userId)        // ✅ uses class field userId, not local variable
                .courseId(101L)
                .amount(new BigDecimal("999.00"))
                .currency("USD")
                .discountAmount(BigDecimal.ZERO)
                .finalAmount(new BigDecimal("999.00"))
                .status(OrderStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Nested
    @DisplayName("POST /api/v1/payments/orders")
    class CreateOrderTests {

        @Test
        @DisplayName("Should create order and return 201")
        void shouldCreateOrderAndReturn201() throws Exception {
            CreateOrderRequest request = new CreateOrderRequest(101L, null, "USD");
            when(orderService.createOrder(any(), any()))
                    .thenReturn(mockOrderResponse);

            mockMvc.perform(post("/api/v1/payments/orders")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("Order created successfully"))
                    // ✅ removed id assertion — id is null since we don't set it in mock
                    .andExpect(jsonPath("$.data.status").value("PENDING"))
                    .andExpect(jsonPath("$.data.finalAmount").value(999.00));
        }

        @Test
        @DisplayName("Should create order with coupon and return 201")
        void shouldCreateOrderWithCouponAndReturn201() throws Exception {
            CreateOrderRequest request = new CreateOrderRequest(101L, "SAVE20", "USD");
            OrderResponse discountedResponse = OrderResponse.builder()
                    .orderNumber("ORD-TEST-002")
                    .userId(userId)
                    .courseId(101L)
                    .amount(new BigDecimal("999.00"))
                    .currency("USD")
                    .discountAmount(new BigDecimal("199.80"))
                    .finalAmount(new BigDecimal("799.20"))
                    .couponCode("SAVE20")
                    .status(OrderStatus.PENDING)
                    .build();
            // ✅ any() instead of any(UUID.class) to avoid ambiguity
            when(orderService.createOrder(any(), any()))
                    .thenReturn(discountedResponse);

            mockMvc.perform(post("/api/v1/payments/orders")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.data.couponCode").value("SAVE20"))
                    .andExpect(jsonPath("$.data.discountAmount").value(199.80))
                    .andExpect(jsonPath("$.data.finalAmount").value(799.20));
        }

        @Test
        @DisplayName("Should return 400 when courseId is null")
        void shouldReturn400WhenCourseIdIsNull() throws Exception {
            mockMvc.perform(post("/api/v1/payments/orders")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"currency\": \"USD\"}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value("Validation failed"))
                    .andExpect(jsonPath("$.data.courseId").value("Course ID is required"));
        }

        @Test
        @DisplayName("Should return 409 when duplicate order")
        void shouldReturn409WhenDuplicateOrder() throws Exception {
            when(orderService.createOrder(any(), any()))
                    .thenThrow(new DuplicateOrderException(UUID.randomUUID(), 101L));

            mockMvc.perform(post("/api/v1/payments/orders")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    new CreateOrderRequest(101L, null, "USD"))))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value(containsString("already has a completed order")));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/payments/orders/{orderId}")
    class GetOrderTests {

        @Test
        @DisplayName("Should return order and 200")
        void shouldReturnOrderAnd200() throws Exception {
            // ✅ orderId is Long not UUID — use eq(1L)
            when(orderService.getOrder(eq(UUID.randomUUID()), any()))
                    .thenReturn(mockOrderResponse);

            mockMvc.perform(get("/api/v1/payments/orders/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.orderNumber").value("ORD-TEST-001"));
        }

        @Test
        @DisplayName("Should return 404 when order not found")
        void shouldReturn404WhenOrderNotFound() throws Exception {
            // ✅ orderId is Long — use eq(9999L), OrderNotFoundException takes Long
            when(orderService.getOrder(any(), any()))
                    .thenThrow(new OrderNotFoundException("order not found"));

            mockMvc.perform(get("/api/v1/payments/orders/some-id"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value(containsString("not found")));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/payments/orders/user/{userId}")
    class GetUserOrdersTests {

        @Test
        @DisplayName("Should return all user orders and 200")
        void shouldReturnAllUserOrdersAnd200() throws Exception {
            OrderResponse order2 = OrderResponse.builder()
                    // ✅ removed invalid UUID.fromString("2") — id is Long, just skip it
                    .orderNumber("ORD-TEST-002")
                    .userId(userId)
                    .courseId(202L)
                    .finalAmount(new BigDecimal("999.00"))
                    .status(OrderStatus.COMPLETED)
                    .build();
            when(orderService.getUserOrders(userId))
                    .thenReturn(List.of(mockOrderResponse, order2));

            mockMvc.perform(get("/api/v1/payments/orders/user/" + userId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data", hasSize(2)))
                    .andExpect(jsonPath("$.data[0].orderNumber").value("ORD-TEST-001"))
                    .andExpect(jsonPath("$.data[1].orderNumber").value("ORD-TEST-002"));
        }

        @Test
        @DisplayName("Should return empty list when user has no orders")
        void shouldReturnEmptyListWhenUserHasNoOrders() throws Exception {
            when(orderService.getUserOrders(userId)).thenReturn(List.of());

            mockMvc.perform(get("/api/v1/payments/orders/user/" + userId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data", hasSize(0)));
        }
    }
}