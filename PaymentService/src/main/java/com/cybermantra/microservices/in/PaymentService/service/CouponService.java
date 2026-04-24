package com.cybermantra.microservices.in.PaymentService.service;

import com.cybermantra.microservices.in.PaymentService.dto.request.CouponValidateRequest;
import com.cybermantra.microservices.in.PaymentService.dto.response.CouponValidateResponse;
import com.cybermantra.microservices.in.PaymentService.entity.Coupon;
import com.cybermantra.microservices.in.PaymentService.enums.DiscountType;
import com.cybermantra.microservices.in.PaymentService.exception.InvalidCouponException;
import com.cybermantra.microservices.in.PaymentService.repository.CouponRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Arrays;

@Service
@RequiredArgsConstructor
@Slf4j
public class CouponService {

    private final CouponRepository couponRepository;

    @Transactional(readOnly = true)
    public CouponValidateResponse validateCoupon(CouponValidateRequest request) {
        Coupon coupon = couponRepository
                .findValidCoupon(request.getCode().toUpperCase(), LocalDateTime.now())
                .orElseThrow(() -> new InvalidCouponException(
                        "Coupon '" + request.getCode() + "' is invalid, expired, or exhausted"));

        // Check if coupon is applicable to this specific course
        if (coupon.getApplicableCourses() != null
                && coupon.getApplicableCourses().length > 0) {
            boolean applicable = Arrays.asList(coupon.getApplicableCourses())
                    .contains(request.getCourseId());
            if (!applicable) {
                throw new InvalidCouponException(
                        "Coupon '" + request.getCode() + "' is not applicable for this course");
            }
        }

        BigDecimal discountAmount = calculateDiscount(
                coupon, request.getOriginalAmount());
        BigDecimal finalAmount = request.getOriginalAmount()
                .subtract(discountAmount)
                .setScale(2, RoundingMode.HALF_UP);

        log.info("Coupon {} validated for course {} — discount: {}",
                request.getCode(), request.getCourseId(), discountAmount);

        return CouponValidateResponse.builder()
                .code(coupon.getCode())
                .discountType(coupon.getDiscountType())
                .discountValue(coupon.getDiscountValue())
                .originalAmount(request.getOriginalAmount())
                .discountAmount(discountAmount)
                .finalAmount(finalAmount)
                .valid(true)
                .message("Coupon applied successfully")
                .build();
    }

    private BigDecimal calculateDiscount(Coupon coupon, BigDecimal originalAmount) {
        if (coupon.getDiscountType() == DiscountType.PERCENTAGE) {
            return originalAmount
                    .multiply(coupon.getDiscountValue())
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        }
        // FIXED — discount can't exceed original price
        return coupon.getDiscountValue().min(originalAmount);
    }
}