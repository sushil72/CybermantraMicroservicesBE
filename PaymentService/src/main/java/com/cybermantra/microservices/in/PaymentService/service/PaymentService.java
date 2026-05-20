package com.cybermantra.microservices.in.PaymentService.service;

import com.cybermantra.microservices.in.PaymentService.dto.request.ProcessPaymentRequest;
import com.cybermantra.microservices.in.PaymentService.dto.response.PaymentResponse;
import com.cybermantra.microservices.in.PaymentService.entity.Order;
import com.cybermantra.microservices.in.PaymentService.entity.Payment;
import com.cybermantra.microservices.in.PaymentService.enums.OrderStatus;
import com.cybermantra.microservices.in.PaymentService.enums.PaymentStatus;
import com.cybermantra.microservices.in.PaymentService.events.PaymentFailedEvents;
import com.cybermantra.microservices.in.PaymentService.events.PaymentSuccessEvent;
import com.cybermantra.microservices.in.PaymentService.exception.OrderAlreadyCompletedException;
import com.cybermantra.microservices.in.PaymentService.exception.PaymentProcessingException;
import com.cybermantra.microservices.in.PaymentService.kafka.PaymentEventProducer;
import com.cybermantra.microservices.in.PaymentService.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final OrderService orderService;
    private final StripeService stripeService;
    private final RazorpayService razorpayService;
    private final PaymentEventProducer eventProducer;

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
            PaymentSuccessEvent successEvent = PaymentSuccessEvent.builder()
                    .eventId(UUID.randomUUID().toString())
                    .eventType("PAYMENT_SUCCESS")
                    .sourceService("payment-service")
                    .timestamp(LocalDateTime.now())
                    .paymentId(payment.getId())
                    .orderId(order.getId())
                    .userId(order.getUserId())
                    .courseId(order.getCourseId())
                    .amount(order.getFinalAmount())
                    .currency(order.getCurrency())
                    .transactionId(transactionId)
                    .paymentGateway(request.getPaymentGateway().name())
                    .build();

            eventProducer.publishPaymentSuccess(successEvent);

            return mapToResponse(payment);

        } catch (Exception ex) {
            // Payment failed — update records
            payment.setStatus(PaymentStatus.FAILED);
            payment.setErrorMessage(ex.getMessage());
            paymentRepository.save(payment);

            orderService.markOrderFailed(order.getId());

            log.error("Payment failed — order: {}, reason: {}",
                    order.getOrderNumber(), ex.getMessage());
            PaymentFailedEvents failedEvent = PaymentFailedEvents.builder()
                    .eventId(UUID.randomUUID().toString())
                    .eventType("PAYMENT_FAILED")
                    .sourceService("payment-service")
                    .timestamp(LocalDateTime.now())
                    .orderId(order.getId())
                    .userId(order.getUserId())
                    .courseId(order.getCourseId())
                    .reason(ex.getMessage())
                    .paymentGateway(request.getPaymentGateway().name())
                    .build();

            eventProducer.publishPaymentFailed(failedEvent);
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