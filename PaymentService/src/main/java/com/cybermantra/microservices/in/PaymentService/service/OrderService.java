package com.cybermantra.microservices.in.PaymentService.service;

import com.cybermantra.microservices.in.PaymentService.dto.request.CreateOrderRequest;
import com.cybermantra.microservices.in.PaymentService.dto.response.OrderResponse;
import com.cybermantra.microservices.in.PaymentService.entity.Coupon;
import com.cybermantra.microservices.in.PaymentService.entity.Order;
import com.cybermantra.microservices.in.PaymentService.enums.OrderStatus;
import com.cybermantra.microservices.in.PaymentService.exception.*;
import com.cybermantra.microservices.in.PaymentService.repository.CouponRepository;
import com.cybermantra.microservices.in.PaymentService.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {

    private final OrderRepository orderRepository;
    private final CouponRepository couponRepository;

    // Platform commission rate — instructor gets 70%, platform keeps 30%
    private static final BigDecimal INSTRUCTOR_REVENUE_SHARE = new BigDecimal("0.70");

    // TODO: Replace with actual price from Course Service via Feign
    private static final BigDecimal MOCK_COURSE_PRICE = new BigDecimal("999.00");

    @Transactional
    public OrderResponse createOrder(UUID userId, CreateOrderRequest request) {

        // Prevent duplicate orders for same course
        if (orderRepository.existsByUserIdAndCourseIdAndStatus(
                userId, request.getCourseId(), OrderStatus.COMPLETED)) {
            throw new DuplicateOrderException(userId, request.getCourseId());
        }

        // TODO: Fetch actual course price from Course Service
        BigDecimal originalAmount = MOCK_COURSE_PRICE;
        BigDecimal discountAmount = BigDecimal.ZERO;
        String couponCode = null;

        // Apply coupon if provided
        if (request.getCouponCode() != null && !request.getCouponCode().isBlank()) {
            Coupon coupon = couponRepository
                    .findValidCoupon(request.getCouponCode(), LocalDateTime.now())
                    .orElseThrow(() -> new InvalidCouponException(
                            "Coupon '" + request.getCouponCode() + "' is invalid or expired"));

            discountAmount = calculateDiscount(coupon, originalAmount);
            couponCode = coupon.getCode();

            // Increment coupon usage count
            coupon.setCurrentUses(coupon.getCurrentUses() + 1);
            couponRepository.save(coupon);
        }

        BigDecimal finalAmount = originalAmount.subtract(discountAmount)
                .setScale(2, RoundingMode.HALF_UP);

        Order order = Order.builder()
                .orderNumber(generateOrderNumber())
                .userId(userId)
                .courseId(request.getCourseId())
                .amount(originalAmount)
                .currency(request.getCurrency())
                .discountAmount(discountAmount)
                .finalAmount(finalAmount)
                .couponCode(couponCode)
                .status(OrderStatus.PENDING)
                .build();

        order = orderRepository.save(order);
        log.info("Order {} created for user {} course {}",
                order.getOrderNumber(), userId, request.getCourseId());

        return mapToResponse(order);
    }

    @Transactional(readOnly = true)
    public OrderResponse getOrder(UUID orderId, UUID userId) {
        Order order = findOrderById(orderId);
        verifyOwnership(order, userId);
        return mapToResponse(order);
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> getUserOrders(UUID userId) {
        return orderRepository.findAllByUserId(userId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public void markOrderCompleted(UUID orderId) {
        Order order = findOrderById(orderId);
        order.setStatus(OrderStatus.COMPLETED);
        order.setCompletedAt(LocalDateTime.now());
        orderRepository.save(order);
        log.info("Order {} marked as COMPLETED", orderId);
    }

    @Transactional
    public void markOrderFailed(UUID orderId) {
        Order order = findOrderById(orderId);
        order.setStatus(OrderStatus.FAILED);
        orderRepository.save(order);
        log.info("Order {} marked as FAILED", orderId);
    }

    @Transactional
    public void markOrderRefunded(UUID orderId) {
        Order order = findOrderById(orderId);
        order.setStatus(OrderStatus.REFUNDED);
        orderRepository.save(order);
        log.info("Order {} marked as REFUNDED", orderId);
    }

    public Order findOrderById(UUID orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));
    }

    private BigDecimal calculateDiscount(Coupon coupon, BigDecimal originalAmount) {
        return switch (coupon.getDiscountType()) {
            case PERCENTAGE -> originalAmount
                    .multiply(coupon.getDiscountValue())
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            case FIXED -> coupon.getDiscountValue()
                    .min(originalAmount); // discount can't exceed original price
        };
    }

    private String generateOrderNumber() {
        return "ORD-" + UUID.randomUUID().toString().toUpperCase().substring(0, 12);
    }

    private void verifyOwnership(Order order, UUID userId) {
        if (!order.getUserId().equals(userId)) {
            throw new AccessDeniedException();
        }
    }

    private OrderResponse mapToResponse(Order order) {
        return OrderResponse.builder()
                .id(order.getId())
                .orderNumber(order.getOrderNumber())
                .userId(order.getUserId())
                .courseId(order.getCourseId())
                .amount(order.getAmount())
                .currency(order.getCurrency())
                .discountAmount(order.getDiscountAmount())
                .finalAmount(order.getFinalAmount())
                .couponCode(order.getCouponCode())
                .status(order.getStatus())
                .paymentMethod(order.getPaymentMethod())
                .createdAt(order.getCreatedAt())
                .completedAt(order.getCompletedAt())
                .build();
    }
}
