package com.cybermantra.microservices.in.PaymentService.controller;

import com.cybermantra.microservices.in.PaymentService.dto.request.CreateOrderRequest;
import com.cybermantra.microservices.in.PaymentService.dto.response.ApiResponse;
import com.cybermantra.microservices.in.PaymentService.dto.response.OrderResponse;
import com.cybermantra.microservices.in.PaymentService.service.OrderService;
import com.cybermantra.microservices.in.PaymentService.utilities.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("${api.baseurl}/orders")
@RequiredArgsConstructor
@Tag(name = "Orders", description = "Order management endpoints")
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    @Operation(summary = "Create a new order", description = "Creates an order before payment is processed. Applies coupon if provided.")
    public ResponseEntity<ApiResponse<OrderResponse>> createOrder(
            @Valid @RequestBody CreateOrderRequest request) {
        System.out.println("order request received:" + request.toString());
        UUID userId = SecurityUtils.getCurrentUserId();
        OrderResponse orderResponse = orderService.createOrder(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Order created successfully", orderResponse));
    }

    @GetMapping("/{orderId}")
    @Operation(summary = "Get order by ID")
    public ResponseEntity<ApiResponse<OrderResponse>> getOrder(
            @PathVariable UUID orderId) {
        UUID userId = SecurityUtils.getCurrentUserId();
        OrderResponse orderResponse = orderService.getOrder(orderId, userId);
        return ResponseEntity.ok(ApiResponse.success(orderResponse));
    }

    @GetMapping("/user/{userId}")
    @Operation(summary = "Get all orders for a user")
    public ResponseEntity<ApiResponse<List<OrderResponse>>> getUserOrders(
            @PathVariable UUID userId) {
        List<OrderResponse> orderResponses = orderService.getUserOrders(userId);
        return ResponseEntity.ok(ApiResponse.success(orderResponses));
    }
}
