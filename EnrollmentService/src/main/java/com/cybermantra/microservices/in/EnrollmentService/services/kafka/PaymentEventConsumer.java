package com.cybermantra.microservices.in.EnrollmentService.services.kafka;


import com.cybermantra.microservices.in.EnrollmentService.events.PaymentSuccessEvent;
import com.cybermantra.microservices.in.EnrollmentService.events.RefundProcessedEvent;
import com.cybermantra.microservices.in.EnrollmentService.services.EnrollmentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentEventConsumer {

    private final EnrollmentService enrollmentService;

    @KafkaListener(
            topics = "payment-events",
            groupId = "enrollment-service-group",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void handlePaymentEvent(
            ConsumerRecord<String, Object> record,
            Acknowledgment acknowledgment) {

        log.info("📨 Kafka event received from topic: payment-events");
        log.info("📦 Partition: {}, Offset: {}, Key: {}",
                record.partition(),
                record.offset(),
                record.key());

        try {

            Object payload = record.value();

            log.info("🔍 Payload type received: {}",
                    payload.getClass().getSimpleName());

            // PAYMENT SUCCESS
            if (payload instanceof PaymentSuccessEvent event) {

                log.info("💰 PAYMENT SUCCESS EVENT RECEIVED");
                log.info("👤 User ID: {}", event.getUserId());
                log.info("📚 Course ID: {}", event.getCourseId());

                handlePaymentSuccess(event);

                log.info("✅ Enrollment created successfully");

            }

            // REFUND
            else if (payload instanceof RefundProcessedEvent event) {

                log.info("💸 REFUND EVENT RECEIVED");
                log.info("👤 User ID: {}", event.getUserId());
                log.info("📚 Course ID: {}", event.getCourseId());

                handleRefundProcessed(event);

                log.info("🗑️ Enrollment removed after refund");
            }

            else {
                log.warn("⚠️ Unknown event received");
            }

            acknowledgment.acknowledge();

            log.info("✅ Kafka offset acknowledged successfully");

        } catch (Exception ex) {

            log.error("❌ Error processing Kafka event");
            log.error("🔥 Exception Message: {}", ex.getMessage(), ex);

            log.warn("🔁 Kafka will retry this event");
        }
    }

    private void handlePaymentSuccess(PaymentSuccessEvent event) {

        log.info("🚀 Starting enrollment process...");

        enrollmentService.createEnrollmentAfterPayment(
                event.getUserId(),
                event.getCourseId()
        );
    }

    private void handleRefundProcessed(RefundProcessedEvent event) {

        log.info("🚀 Starting unenrollment process after refund...");

        enrollmentService.unenrollAfterRefund(
                event.getUserId(),
                event.getCourseId()
        );
    }
}