package com.cybermantra.microservices.in.PaymentService.controller;

import com.cybermantra.microservices.in.PaymentService.dto.request.CouponValidateRequest;
import com.cybermantra.microservices.in.PaymentService.dto.response.CouponValidateResponse;
import com.cybermantra.microservices.in.PaymentService.enums.DiscountType;
import com.cybermantra.microservices.in.PaymentService.exception.GlobalExceptionHandler;
import com.cybermantra.microservices.in.PaymentService.exception.InvalidCouponException;
import com.cybermantra.microservices.in.PaymentService.service.CouponService;
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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CouponController Tests")
class CouponControllerTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @Mock
    private CouponService couponService;

    @InjectMocks
    private CouponController couponController;

    //prevents JwtServiceImpl from being instantiated
    @Mock
    private JwtServiceImpl jwtService;


    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(couponController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules();
    }

    @Test
    @DisplayName("Should validate coupon and return 200 with discount details")
    void shouldValidateCouponAndReturn200() throws Exception {
        // Arrange
        CouponValidateRequest request = new CouponValidateRequest("SAVE20", 101L, new BigDecimal("999.00"));
        CouponValidateResponse response = CouponValidateResponse.builder()
                .code("SAVE20").discountType(DiscountType.PERCENTAGE)
                .discountValue(new BigDecimal("20"))
                .originalAmount(new BigDecimal("999.00"))
                .discountAmount(new BigDecimal("199.80"))
                .finalAmount(new BigDecimal("799.20"))
                .valid(true).message("Coupon applied successfully").build();
        when(couponService.validateCoupon(any(CouponValidateRequest.class))).thenReturn(response);

        // Act & Assert
        mockMvc.perform(post("/api/v1/payments/coupons/validate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.code").value("SAVE20"))
                .andExpect(jsonPath("$.data.discountType").value("PERCENTAGE"))
                .andExpect(jsonPath("$.data.discountAmount").value(199.80))
                .andExpect(jsonPath("$.data.finalAmount").value(799.20))
                .andExpect(jsonPath("$.data.valid").value(true));
    }

    @Test
    @DisplayName("Should return 400 for invalid coupon")
    void shouldReturn400ForInvalidCoupon() throws Exception {
        // Arrange
        CouponValidateRequest request = new CouponValidateRequest("FAKECODE", 101L, new BigDecimal("999.00"));
        when(couponService.validateCoupon(any(CouponValidateRequest.class)))
                .thenThrow(new InvalidCouponException("Coupon 'FAKECODE' is invalid, expired, or exhausted"));

        // Act & Assert
        mockMvc.perform(post("/api/v1/payments/coupons/validate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Coupon 'FAKECODE' is invalid, expired, or exhausted"));
    }

    @Test
    @DisplayName("Should return 400 when coupon code is blank")
    void shouldReturn400WhenCouponCodeIsBlank() throws Exception {
        // Arrange
        String invalidJson = "{\"code\": \"\", \"courseId\": 101, \"originalAmount\": 999.00}";

        // Act & Assert
        mockMvc.perform(post("/api/v1/payments/coupons/validate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.data.code").value("Coupon code is required"));
    }

    @Test
    @DisplayName("Should return 400 when originalAmount is missing")
    void shouldReturn400WhenOriginalAmountIsMissing() throws Exception {
        // Arrange
        String invalidJson = "{\"code\": \"SAVE20\", \"courseId\": 101}";

        // Act & Assert
        mockMvc.perform(post("/api/v1/payments/coupons/validate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.data.originalAmount").value("Original amount is required"));
    }
}