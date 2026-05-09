package com.dishari.in.infrastructure.messaging.producer;


import com.dishari.in.infrastructure.messaging.event.LinkMetadataEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Component
@RequiredArgsConstructor
@Slf4j
public class LinkMetadataEventProducer {
    // Spring will inject the correct bean by type/name
    private final KafkaTemplate<String, LinkMetadataEvent> linkMetadataEventKafkaTemplate;

    @Value("${app.kafka.topics.link-metadata-generation}")
    private String topic;

    public void publishLinkMetadataEvent(String originalUrl) {
        // 1. Decouple from JPA Entity: Use userId instead of the full User object
        LinkMetadataEvent event = new LinkMetadataEvent(originalUrl);
        String key = UUID.randomUUID().toString() ;
        CompletableFuture<SendResult<String, LinkMetadataEvent>> future = linkMetadataEventKafkaTemplate.send(topic, key , event);

        future.whenComplete((result, ex) -> {
            if (ex != null) {
                log.error("Failed to publish link metadata event to Kafka: originalUrl={}, reason={}",
                        originalUrl , ex.getMessage());
                // Critical: In a real system, you might want to update a
                // status in Redis here to "FAILED_TO_QUEUE"
            } else {
                log.info("Link metadata event published to topic [{}] partition [{}] for originalUrl={}",
                        result.getRecordMetadata().topic(),
                        result.getRecordMetadata().partition(),
                        originalUrl);
            }
        });
    }
}
