package com.dishari.in.infrastructure.messaging.producer;

import com.dishari.in.domain.entity.User;
import com.dishari.in.infrastructure.messaging.event.CreateBulkUrlEvent;
import com.dishari.in.infrastructure.messaging.event.QrGenerationEvent;
import com.dishari.in.web.dto.request.CreateBulkUrlRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

@Component
@RequiredArgsConstructor
@Slf4j
public class CreateBulkUrlProducer {

    // Spring will inject the correct bean by type/name
    private final KafkaTemplate<String, CreateBulkUrlEvent> bulkKafkaTemplate;

    @Value("${app.kafka.topics.bulk-url-creation}")
    private String topic;

    public void publishBulkUrlEvent(User user, CreateBulkUrlRequest request) {
        // 1. Decouple from JPA Entity: Use userId instead of the full User object
        CreateBulkUrlEvent event = new CreateBulkUrlEvent(
                user,
                request.urls()
        );

        // 2. Partitioning Strategy: Use userId as the key.
        String key = user.getId().toString();

        CompletableFuture<SendResult<String, CreateBulkUrlEvent>> future = bulkKafkaTemplate.send(topic, key, event);

        future.whenComplete((result, ex) -> {
            if (ex != null) {
                log.error("Failed to publish bulk event to Kafka: userId={}, reason={}",
                        user.getId(), ex.getMessage());
                // Critical: In a real system, you might want to update a
                // status in Redis here to "FAILED_TO_QUEUE"
            } else {
                log.info("Bulk event published to topic [{}] partition [{}] for userId={}",
                        result.getRecordMetadata().topic(),
                        result.getRecordMetadata().partition(),
                        user.getId());
            }
        });
    }
}
