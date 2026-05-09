package com.dishari.in.infrastructure.messaging.producer;


import com.dishari.in.infrastructure.messaging.event.ClickEventGenerationEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Component
@RequiredArgsConstructor
@Slf4j
public class ClickEventProducer {

    private final KafkaTemplate <String , ClickEventGenerationEvent> clickEventKafkaTemplate;

    @Value("${app.kafka.topics.click-event-generation}")
    private String topic ;

    public void publishClickEvent(String shortUrlId , String ipAddress , String userAgent , UUID variantId , String referer) {
        ClickEventGenerationEvent event = ClickEventGenerationEvent.builder()
                .shortUrlId(shortUrlId)
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .variantId(variantId)
                .referer(referer)
                .occurredAt(Instant.now())
                .build();

        //Publishing the event
        CompletableFuture<SendResult<String , ClickEventGenerationEvent>> future = clickEventKafkaTemplate.send(topic , event) ;

        future.whenComplete((result, ex) -> {
            if (ex != null) {
                log.error("Failed to publish click event to Kafka: shortUrlId={}, reason={}",
                       shortUrlId, ex.getMessage());
                // Critical: In a real system, you might want to update a
                // status in Redis here to "FAILED_TO_QUEUE"
            } else {
                log.info("Click event published to topic [{}] partition [{}] for shortUrlId={}",
                        result.getRecordMetadata().topic(),
                        result.getRecordMetadata().partition(),
                        shortUrlId);
            }
        });
    }

}
