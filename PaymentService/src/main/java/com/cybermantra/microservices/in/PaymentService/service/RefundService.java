package com.cybermantra.microservices.in.PaymentService.service;

import com.cybermantra.microservices.in.PaymentService.dto.request.RefundRequest;
import com.cybermantra.microservices.in.PaymentService.dto.response.RefundResponse;
import com.cybermantra.microservices.in.PaymentService.entity.Order;
import com.cybermantra.microservices.in.PaymentService.entity.Refund;
import com.cybermantra.microservices.in.PaymentService.enums.OrderStatus;
import com.cybermantra.microservices.in.PaymentService.enums.RefundStatus;
import com.cybermantra.microservices.in.PaymentService.exception.AccessDeniedException;
import com.cybermantra.microservices.in.PaymentService.exception.RefundNotEligibleException;
import com.cybermantra.microservices.in.PaymentService.exception.RefundNotFoundException;
import com.cybermantra.microservices.in.PaymentService.repository.RefundRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class RefundService {

    private final RefundRepository refundRepository;
    private final OrderService orderService;

    private static final int REFUND_WINDOW_DAYS = 30;

    @Transactional
    public RefundResponse requestRefund(UUID userId, RefundRequest request) {
        Order order = orderService.findOrderById(request.getOrderId());

        // Only the order owner can request refund
        if (!order.getUserId().equals(userId)) {
            throw new AccessDeniedException();
        }

        // Only completed orders can be refunded
        if (order.getStatus() != OrderStatus.COMPLETED) {
            throw new RefundNotEligibleException(
                    "Only completed orders are eligible for refund");
        }

        // Check 30-day refund window
        if (order.getCompletedAt().isBefore(
                LocalDateTime.now().minusDays(REFUND_WINDOW_DAYS))) {
            throw new RefundNotEligibleException(
                    "Refund window of " + REFUND_WINDOW_DAYS + " days has expired");
        }

        // Prevent duplicate refund request
        if (refundRepository.existsByOrderIdAndStatusNot(
                order.getId (), RefundStatus.REJECTED)) {
            throw new RefundNotEligibleException(
                    "A refund request already exists for this order");
        }

        // Refund amount cannot exceed order final amount
        if (request.getRefundAmount().compareTo(order.getFinalAmount()) > 0) {
            throw new RefundNotEligibleException(
                    "Refund amount cannot exceed order amount of "
                            + order.getFinalAmount());
        }

        Refund refund = Refund.builder()
                .order(order)
                .refundAmount(request.getRefundAmount())
                .reason(request.getReason())
                .requestedBy(userId)
                .status(RefundStatus.PENDING)
                .build();

        refund = refundRepository.save(refund);
        log.info("Refund request {} created for order {}",
                refund.getId(), order.getOrderNumber());

        return mapToResponse(refund);
    }

    @Transactional(readOnly = true)
    public RefundResponse getRefund(UUID refundId, UUID userId) {
        Refund refund = refundRepository.findById(refundId)
                .orElseThrow(() -> new RefundNotFoundException(refundId));

        if (!refund.getRequestedBy().equals(userId)) {
            throw new AccessDeniedException();
        }

        return mapToResponse(refund);
    }

    private RefundResponse mapToResponse(Refund refund) {
        return RefundResponse.builder()
                .id(refund.getId())
                .orderId(refund.getOrder().getId())
                .refundAmount(refund.getRefundAmount())
                .reason(refund.getReason())
                .status(refund.getStatus())
                .requestedBy(refund.getRequestedBy())
                .requestedAt(refund.getRequestedAt())
                .processedAt(refund.getProcessedAt())
                .build();
    }
}
