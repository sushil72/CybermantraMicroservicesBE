package com.cybermantra.microservices.in.PaymentService.controller;

import com.cybermantra.microservices.in.PaymentService.dto.request.RefundRequest;
import com.cybermantra.microservices.in.PaymentService.dto.response.RefundResponse;
import com.cybermantra.microservices.in.PaymentService.enums.RefundStatus;
import com.cybermantra.microservices.in.PaymentService.exception.GlobalExceptionHandler;
import com.cybermantra.microservices.in.PaymentService.exception.RefundNotEligibleException;
import com.cybermantra.microservices.in.PaymentService.service.RefundService;
import com.cybermantra.microservices.in.PaymentService.service.jwt.JwtServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
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

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RefundController Tests")
class RefundControllerTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @Mock
    private RefundService refundService;

    @Mock
    private JwtServiceImpl jwtService;

    @InjectMocks
    private RefundController refundController;

    private UUID userId;
    private RefundResponse mockRefundResponse;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(refundController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules();

        userId = UUID.fromString("1035beb8-307a-4379-bc2c-97157aa78bbf");
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(userId.toString(), null,
                        List.of(new SimpleGrantedAuthority("ROLE_USER"))));
        UUID userId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        mockRefundResponse = RefundResponse.builder()
                .id(userId).orderId(orderId).refundAmount(new BigDecimal("999.00"))
                .reason("Course not as described").status(RefundStatus.PENDING)
                .requestedBy(userId).requestedAt(LocalDateTime.now()).build();
    }

    @Test
    @DisplayName("Should submit refund request and return 201")
    void shouldSubmitRefundRequestAndReturn201() throws Exception {
        // Arrange
        UUID orderId = UUID.randomUUID();
        RefundRequest request = new RefundRequest(orderId, new BigDecimal("999.00"), "Course not as described");
        when(refundService.requestRefund(any(UUID.class), any(RefundRequest.class)))
                .thenReturn(mockRefundResponse);

        // Act & Assert
        mockMvc.perform(post("/api/v1/payments/refunds")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Refund request submitted"))
                .andExpect(jsonPath("$.data.status").value("PENDING"))
                .andExpect(jsonPath("$.data.refundAmount").value(999.00));
    }

    @Test
    @DisplayName("Should return 400 when outside refund window")
    void shouldReturn400WhenOutsideRefundWindow() throws Exception {
        // Arrange
        UUID orderId = UUID.randomUUID();
        RefundRequest request = new RefundRequest(orderId, new BigDecimal("999.00"), "Late request");
        when(refundService.requestRefund(any(UUID.class), any(RefundRequest.class)))
                .thenThrow(new RefundNotEligibleException("Refund window of 30 days has expired"));

        // Act & Assert
        mockMvc.perform(post("/api/v1/payments/refunds")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Refund window of 30 days has expired"));
    }

    @Test
    @DisplayName("Should return 400 when reason is blank")
    void shouldReturn400WhenReasonIsBlank() throws Exception {
        // Arrange
        String invalidJson = "{\"orderId\": 1, \"refundAmount\": 999.00, \"reason\": \"\"}";

        // Act & Assert
        mockMvc.perform(post("/api/v1/payments/refunds")
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.data.reason").value("Reason is required"));
    }

    @Test
    @DisplayName("Should get refund by ID and return 200")
    void shouldGetRefundByIdAndReturn200() throws Exception {
        // Arrange
        UUID refundId = UUID.randomUUID();
        when(refundService.getRefund(eq(refundId), any(UUID.class))).thenReturn(mockRefundResponse);

        // Act & Assert
        mockMvc.perform(get("/api/v1/payments/refunds/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.status").value("PENDING"));
    }
}