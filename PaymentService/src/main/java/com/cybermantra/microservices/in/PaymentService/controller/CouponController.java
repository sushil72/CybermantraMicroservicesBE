package com.cybermantra.microservices.in.PaymentService.controller;
import com.cybermantra.microservices.in.PaymentService.dto.request.CouponValidateRequest;
import com.cybermantra.microservices.in.PaymentService.dto.response.ApiResponse;
import com.cybermantra.microservices.in.PaymentService.dto.response.CouponValidateResponse;
import com.cybermantra.microservices.in.PaymentService.service.CouponService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("${api.baseurl}/coupons")
@RequiredArgsConstructor
@Tag(name = "Coupons", description = "Coupon validation endpoints")
public class CouponController {
    private final CouponService couponService;

    @PostMapping("/validate")
    @Operation(summary = "Validate a coupon code", description = "Validates a coupon and returns the discount amount and final price.")
    public ResponseEntity<ApiResponse<CouponValidateResponse>> validateCoupon(
            @Valid @RequestBody CouponValidateRequest request) {
        System.out.println("Request received: " + request.getCode() + " for course " + request.getCourseId() );
        CouponValidateResponse couponValidateResponse = couponService.validateCoupon(request);
        return ResponseEntity.ok(ApiResponse.success(couponValidateResponse));
    }
}
