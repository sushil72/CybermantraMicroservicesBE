package com.cybermantra.microservices.in.PaymentService.controller;

import com.cybermantra.microservices.in.PaymentService.dto.request.RefundRequest;
import com.cybermantra.microservices.in.PaymentService.dto.response.ApiResponse;
import com.cybermantra.microservices.in.PaymentService.dto.response.RefundResponse;
import com.cybermantra.microservices.in.PaymentService.service.RefundService;
import com.cybermantra.microservices.in.PaymentService.utilities.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("${api.baseurl}/refunds")
@RequiredArgsConstructor
@Tag(name = "Refunds", description = "Refund management endpoints")
public class RefundController {
    private final RefundService refundService;
    @PostMapping
    @Operation(summary = "Request a refund", description = "Submits a refund request. Only allowed within 30 days of purchase.")
    public ResponseEntity<ApiResponse<RefundResponse>> requestRefund(@Valid @RequestBody RefundRequest request) {
        UUID userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Refund request submitted", refundService.requestRefund(userId, request)));
    }
    @GetMapping("/{refundId}")
    @Operation(summary = "Get refund by ID")
    public ResponseEntity<ApiResponse<RefundResponse>> getRefund(@PathVariable UUID refundId) {
        UUID userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.success(refundService.getRefund(refundId, userId)));
    }
}
