package com.cybermantra.microservices.in.PaymentService.controller;

import com.cybermantra.microservices.in.PaymentService.dto.response.ApiResponse;
import com.cybermantra.microservices.in.PaymentService.dto.response.RevenueResponse;
import com.cybermantra.microservices.in.PaymentService.service.RevenueService;
import com.cybermantra.microservices.in.PaymentService.utilities.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping("${api.baseurl}/instructor")
@RequiredArgsConstructor
@Tag(name = "Revenue", description = "Instructor revenue and payout endpoints")
public class RevenueController {
    private final RevenueService revenueService;
    @GetMapping("/revenue")
    @Operation(summary = "Get instructor revenue", description = "Returns revenue breakdown for the authenticated instructor for a given period.")
    public ResponseEntity<ApiResponse<RevenueResponse>> getRevenue(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        UUID instructorId = SecurityUtils.getCurrentUserId();

        RevenueResponse revenueResponse = revenueService.getRevenue(instructorId, from, to);
        return ResponseEntity.ok(ApiResponse.success(revenueResponse));
    }
}

/*
```

        ---

        ## Current State Summary
```
        ✅ Entities     — Order, Payment, Refund, Coupon, InstructorPayout
✅ Enums        — OrderStatus, PaymentStatus, RefundStatus,
PaymentGateway, DiscountType, PayoutStatus
✅ DTOs         — All request + response DTOs
✅ Controllers  — All 5 controllers with correct endpoints
⬜ Repositories — Next step
⬜ Services     — After repositories
⬜ Exceptions   — After services

 */
