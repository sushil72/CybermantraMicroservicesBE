package com.cybermantra.microservices.in.PaymentService.kafka;

import com.cybermantra.microservices.in.PaymentService.events.PaymentFailedEvents;
import com.cybermantra.microservices.in.PaymentService.events.PaymentSuccessEvent;
import com.cybermantra.microservices.in.PaymentService.events.RefundProcessedEvents;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentEventProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    private static final String PAYMENT_TOPIC = "payment-events";

    public void publishPaymentSuccess(PaymentSuccessEvent event) {
        String key = event.getUserId().toString();

        CompletableFuture<SendResult<String, Object>> future =
                kafkaTemplate.send(PAYMENT_TOPIC, key, event);

        future.whenComplete((result, ex) -> {
            if (ex == null) {
                log.info("✅ PaymentSuccessEvent published — " +
                                "userId: {}, courseId: {}, partition: {}, offset: {}",
                        event.getUserId(),
                        event.getCourseId(),
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset());
            } else {
                log.error("❌ Failed to publish PaymentSuccessEvent — " +
                                "userId: {}, error: {}",
                        event.getUserId(), ex.getMessage());
            }
        });
    }

    public void publishPaymentFailed(PaymentFailedEvents event) {
        String key = event.getUserId().toString();
        kafkaTemplate.send(PAYMENT_TOPIC, key, event)
                .whenComplete((result, ex) -> {
                    if (ex == null) {
                        log.info("✅ PaymentFailedEvent published — userId: {}", event.getUserId());
                    } else {
                        log.error("❌ Failed to publish PaymentFailedEvent: {}", ex.getMessage());
                    }
                });
    }

    public void publishRefundProcessed(RefundProcessedEvents event) {
        String key = event.getUserId().toString();
        kafkaTemplate.send(PAYMENT_TOPIC, key, event)
                .whenComplete((result, ex) -> {
                    if (ex == null) {
                        log.info("✅ RefundProcessedEvent published — userId: {}", event.getUserId());
                    } else {
                        log.error("❌ Failed to publish RefundProcessedEvent: {}", ex.getMessage());
                    }
                });
    }
}