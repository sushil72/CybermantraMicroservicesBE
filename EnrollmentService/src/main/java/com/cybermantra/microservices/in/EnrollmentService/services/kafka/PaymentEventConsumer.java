package com.cybermantra.microservices.in.EnrollmentService.services.kafka;


import com.cybermantra.microservices.in.EnrollmentService.events.PaymentSuccessEvent;
import com.cybermantra.microservices.in.EnrollmentService.events.RefundProcessedEvent;
import com.cybermantra.microservices.in.EnrollmentService.services.EnrollmentService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentEventConsumer {

    private final EnrollmentService enrollmentService;
    private final ObjectMapper objectMapper;

    @KafkaListener(
            topics = "payment-events",
            groupId = "enrollment-service-group",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void handlePaymentEvent(
            ConsumerRecord<String, Object> record,
            Acknowledgment acknowledgment) {

        log.info("📨 Received event — partition: {}, offset: {}, key: {}",
                record.partition(), record.offset(), record.key());

        try {
            // Deserialize as Map to read eventType first
            @SuppressWarnings("unchecked")
            Map<String, Object> payload = (Map<String, Object>) record.value();

            String eventType = (String) payload.get("eventType");
            log.info("🔔 Event type: {}", eventType);

            switch (eventType) {

                case "PAYMENT_SUCCESS" -> {
                    PaymentSuccessEvent event = objectMapper
                            .convertValue(payload, PaymentSuccessEvent.class);
                    log.info("✅ Processing PAYMENT_SUCCESS — userId: {}, courseId: {}",
                            event.getUserId(), event.getCourseId());
                    handlePaymentSuccess(event);
                }

                case "PAYMENT_FAILED" -> {
                    // ✅ Log it but DO NOT enroll
                    log.warn("⚠️ PAYMENT_FAILED event received — " +
                                    "userId: {}, courseId: {} — skipping enrollment",
                            payload.get("userId"), payload.get("courseId"));
                }

                case "REFUND_PROCESSED" -> {
                    RefundProcessedEvent event = objectMapper
                            .convertValue(payload, RefundProcessedEvent.class);
                    log.info("🔄 Processing REFUND_PROCESSED — userId: {}, courseId: {}",
                            event.getUserId(), event.getCourseId());
                    handleRefundProcessed(event);
                }

                default -> log.warn("⚠️ Unknown eventType: {} — ignoring", eventType);
            }

            // Commit offset only after successful processing
            acknowledgment.acknowledge();
            log.info("✅ Offset {} acknowledged", record.offset());

        } catch (Exception ex) {
            log.error("❌ Failed to process event at offset: {} — error: {}",
                    record.offset(), ex.getMessage());
            // Do NOT acknowledge — Kafka will redeliver
        }
    }

    private void handlePaymentSuccess(PaymentSuccessEvent event) {
        enrollmentService.createEnrollmentAfterPayment(
                event.getUserId(),
                event.getCourseId()
        );
    }

    private void handleRefundProcessed(RefundProcessedEvent event) {
        enrollmentService.unenrollAfterRefund(
                event.getUserId(),
                event.getCourseId()
        );
    }
}