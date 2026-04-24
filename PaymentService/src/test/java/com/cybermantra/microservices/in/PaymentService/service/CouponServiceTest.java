package com.cybermantra.microservices.in.PaymentService.service;

import com.cybermantra.microservices.in.PaymentService.dto.request.CouponValidateRequest;
import com.cybermantra.microservices.in.PaymentService.dto.response.CouponValidateResponse;
import com.cybermantra.microservices.in.PaymentService.entity.Coupon;
import com.cybermantra.microservices.in.PaymentService.enums.DiscountType;
import com.cybermantra.microservices.in.PaymentService.exception.InvalidCouponException;
import com.cybermantra.microservices.in.PaymentService.repository.CouponRepository;
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

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CouponService Tests")
class CouponServiceTest {

    @Mock
    private CouponRepository couponRepository;

    @InjectMocks
    private CouponService couponService;

    private Coupon percentageCoupon;
    private Coupon fixedCoupon;

    @BeforeEach
    void setUp() {
        percentageCoupon = Coupon.builder()
                .code("SAVE20")
                .discountType(DiscountType.PERCENTAGE)
                .discountValue(new BigDecimal("20"))
                .currentUses(0).maxUses(100).isActive(true)
                .validFrom(LocalDateTime.now().minusDays(1))
                .validUntil(LocalDateTime.now().plusDays(30))
                .build();

        fixedCoupon = Coupon.builder()
                .code("FLAT100")
                .discountType(DiscountType.FIXED)
                .discountValue(new BigDecimal("100.00"))
                .currentUses(0).maxUses(50).isActive(true)
                .validFrom(LocalDateTime.now().minusDays(1))
                .validUntil(LocalDateTime.now().plusDays(30))
                .build();
    }

    @Nested
    @DisplayName("Validate Coupon Tests")
    class ValidateCouponTests {

        @Test
        @DisplayName("Should validate PERCENTAGE coupon and calculate correct discount")
        void shouldValidatePercentageCouponCorrectly() {
            // Arrange
            CouponValidateRequest request = new CouponValidateRequest("SAVE20", 101L, new BigDecimal("999.00"));
            when(couponRepository.findValidCoupon(eq("SAVE20"), any(LocalDateTime.class)))
                    .thenReturn(Optional.of(percentageCoupon));

            // Act
            CouponValidateResponse response = couponService.validateCoupon(request);

            // Assert
            assertThat(response.isValid()).isTrue();
            assertThat(response.getCode()).isEqualTo("SAVE20");
            assertThat(response.getDiscountType()).isEqualTo(DiscountType.PERCENTAGE);
            assertThat(response.getOriginalAmount()).isEqualByComparingTo(new BigDecimal("999.00"));
            assertThat(response.getDiscountAmount()).isEqualByComparingTo(new BigDecimal("199.80"));
            assertThat(response.getFinalAmount()).isEqualByComparingTo(new BigDecimal("799.20"));
            assertThat(response.getMessage()).isEqualTo("Coupon applied successfully");
        }

        @Test
        @DisplayName("Should validate FIXED coupon and calculate correct discount")
        void shouldValidateFixedCouponCorrectly() {
            // Arrange
            CouponValidateRequest request = new CouponValidateRequest("FLAT100", 101L, new BigDecimal("999.00"));
            when(couponRepository.findValidCoupon(eq("FLAT100"), any(LocalDateTime.class)))
                    .thenReturn(Optional.of(fixedCoupon));

            // Act
            CouponValidateResponse response = couponService.validateCoupon(request);

            // Assert
            assertThat(response.isValid()).isTrue();
            assertThat(response.getDiscountAmount()).isEqualByComparingTo(new BigDecimal("100.00"));
            assertThat(response.getFinalAmount()).isEqualByComparingTo(new BigDecimal("899.00"));
        }

        @Test
        @DisplayName("Should not allow fixed discount to exceed original amount")
        void shouldCapFixedDiscountAtOriginalAmount() {
            // Arrange - coupon worth more than course price
            Coupon bigDiscountCoupon = Coupon.builder()
                    .code("BIG500")
                    .discountType(DiscountType.FIXED)
                    .discountValue(new BigDecimal("500.00"))
                    .currentUses(0).isActive(true)
                    .validFrom(LocalDateTime.now().minusDays(1))
                    .validUntil(LocalDateTime.now().plusDays(30))
                    .build();
            // Course costs only 100
            CouponValidateRequest request = new CouponValidateRequest("BIG500", 101L, new BigDecimal("100.00"));
            when(couponRepository.findValidCoupon(eq("BIG500"), any(LocalDateTime.class)))
                    .thenReturn(Optional.of(bigDiscountCoupon));

            // Act
            CouponValidateResponse response = couponService.validateCoupon(request);

            // Assert - discount capped at original amount, final amount = 0
            assertThat(response.getDiscountAmount()).isEqualByComparingTo(new BigDecimal("100.00"));
            assertThat(response.getFinalAmount()).isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("Should throw InvalidCouponException for expired coupon")
        void shouldThrowExceptionForExpiredCoupon() {
            // Arrange
            CouponValidateRequest request = new CouponValidateRequest("EXPIRED", 101L, new BigDecimal("999.00"));
            when(couponRepository.findValidCoupon(eq("EXPIRED"), any(LocalDateTime.class)))
                    .thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> couponService.validateCoupon(request))
                    .isInstanceOf(InvalidCouponException.class)
                    .hasMessageContaining("invalid, expired, or exhausted");
        }

        @Test
        @DisplayName("Should throw InvalidCouponException when coupon not applicable for course")
        void shouldThrowExceptionWhenCouponNotApplicableForCourse() {
            // Arrange - coupon only for course 999, not 101
            Coupon restrictedCoupon = Coupon.builder()
                    .code("COURSE999")
                    .discountType(DiscountType.PERCENTAGE)
                    .discountValue(new BigDecimal("10"))
                    .applicableCourses(new Long[]{999L})
                    .currentUses(0).isActive(true)
                    .validFrom(LocalDateTime.now().minusDays(1))
                    .validUntil(LocalDateTime.now().plusDays(30))
                    .build();

            CouponValidateRequest request = new CouponValidateRequest("COURSE999", 101L, new BigDecimal("999.00"));
            when(couponRepository.findValidCoupon(eq("COURSE999"), any(LocalDateTime.class)))
                    .thenReturn(Optional.of(restrictedCoupon));

            // Act & Assert
            assertThatThrownBy(() -> couponService.validateCoupon(request))
                    .isInstanceOf(InvalidCouponException.class)
                    .hasMessageContaining("not applicable for this course");
        }

        @Test
        @DisplayName("Should allow coupon when applicableCourses is null (applies to all)")
        void shouldAllowCouponWhenApplicableCoursesIsNull() {
            // Arrange - null means applies to all courses
            CouponValidateRequest request = new CouponValidateRequest("SAVE20", 101L, new BigDecimal("999.00"));
            when(couponRepository.findValidCoupon(eq("SAVE20"), any(LocalDateTime.class)))
                    .thenReturn(Optional.of(percentageCoupon)); // percentageCoupon has no restrictions

            // Act
            CouponValidateResponse response = couponService.validateCoupon(request);

            // Assert
            assertThat(response.isValid()).isTrue();
        }
    }
}