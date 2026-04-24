package com.cybermantra.microservices.in.PaymentService.service;

import com.cybermantra.microservices.in.PaymentService.dto.request.ProcessPaymentRequest;
import com.cybermantra.microservices.in.PaymentService.dto.response.PaymentResponse;
import com.cybermantra.microservices.in.PaymentService.entity.Order;
import com.cybermantra.microservices.in.PaymentService.entity.Payment;
import com.cybermantra.microservices.in.PaymentService.enums.OrderStatus;
import com.cybermantra.microservices.in.PaymentService.enums.PaymentStatus;
import com.cybermantra.microservices.in.PaymentService.exception.OrderAlreadyCompletedException;
import com.cybermantra.microservices.in.PaymentService.exception.PaymentProcessingException;
import com.cybermantra.microservices.in.PaymentService.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final OrderService orderService;
    private final StripeService stripeService;
    private final RazorpayService razorpayService;

    @Transactional
    public PaymentResponse processPayment(ProcessPaymentRequest request) {

        Order order = orderService.findOrderById(request.getOrderId());

        // Prevent double payment
        if (order.getStatus() == OrderStatus.COMPLETED) {
            throw new OrderAlreadyCompletedException(order.getId());
        }

        // Create payment record with PENDING status
        Payment payment = Payment.builder()
                .order(order)
                .paymentGateway(request.getPaymentGateway())
                .amount(order.getFinalAmount())
                .currency(order.getCurrency())
                .status(PaymentStatus.PENDING)
                .build();

        payment = paymentRepository.save(payment);

        try {
            // Route to correct gateway
            System.out.println("Selected Payment Gateway: " + request.getPaymentGateway());
            String transactionId = switch (request.getPaymentGateway()) {
                case STRIPE -> stripeService.charge(
                        request.getPaymentToken(),
                        order.getFinalAmount(),
                        order.getCurrency(),
                        order.getOrderNumber());
                case RAZORPAY -> {
                    // 1. Verify payment (MANDATORY)
                    razorpayService.verifyPayment(
                            request.getRazorpayOrderId(),
                            request.getPaymentToken(), // paymentId
                            request.getRazorpaySignature()
                    );

                    // 2. Capture payment
                    yield razorpayService.charge(
                            request.getPaymentToken(),
                            order.getFinalAmount(),
                            order.getCurrency(),
                            order.getOrderNumber()
                    );
                }
            };

            // Payment succeeded
            payment.setTransactionId(transactionId);
            payment.setStatus(PaymentStatus.SUCCESS);
            payment.setPaymentMethodDetails(
                    java.util.Map.of("method", request.getPaymentMethod()));
            paymentRepository.save(payment);

            // Update order status
            order.setPaymentMethod(request.getPaymentMethod());
            orderService.markOrderCompleted(order.getId());

            log.info("Payment successful — order: {}, transaction: {}",
                    order.getOrderNumber(), transactionId);

            // TODO: Publish PaymentSuccessEvent → Enrollment Service auto-enrolls user

            return mapToResponse(payment);

        } catch (Exception ex) {
            // Payment failed — update records
            payment.setStatus(PaymentStatus.FAILED);
            payment.setErrorMessage(ex.getMessage());
            paymentRepository.save(payment);

            orderService.markOrderFailed(order.getId());

            log.error("Payment failed — order: {}, reason: {}",
                    order.getOrderNumber(), ex.getMessage());

            throw new PaymentProcessingException(
                    "Payment failed: " + ex.getMessage(), ex);
        }
    }

    private PaymentResponse mapToResponse(Payment payment) {
        return PaymentResponse.builder()
                .id(payment.getId())
                .orderId(payment.getOrder().getId())
                .paymentGateway(payment.getPaymentGateway())
                .transactionId(payment.getTransactionId())
                .amount(payment.getAmount())
                .currency(payment.getCurrency())
                .status(payment.getStatus())
                .errorMessage(payment.getErrorMessage())
                .createdAt(payment.getCreatedAt())
                .build();
    }
}