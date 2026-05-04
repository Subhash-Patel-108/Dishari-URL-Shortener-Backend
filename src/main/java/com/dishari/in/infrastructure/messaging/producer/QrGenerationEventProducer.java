package com.dishari.in.infrastructure.messaging.producer;

import com.dishari.in.infrastructure.messaging.event.QrGenerationEvent;
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
public class QrGenerationEventProducer {

    private final KafkaTemplate<String, QrGenerationEvent> kafkaTemplate;

    @Value("${app.kafka.topics.qr-generation}")
    private String topic;

    public void publishQrGenerationEvent(
            UUID shortUrlId,
            String slug,
            String shortUrlString,
            UUID userId,
            int size,
            String fgColor,
            String bgColor,
            String logoUrl,
            String format) {

        QrGenerationEvent event = QrGenerationEvent.builder()
                .shortUrlId(shortUrlId)
                .slug(slug)
                .shortUrlString(shortUrlString)
                .userId(userId)
                .size(size)
                .fgColor(fgColor)
                .bgColor(bgColor)
                .logoUrl(logoUrl)
                .format(format)
                .triggeredAt(Instant.now())
                .attempt(1)
                .build();

        // Use slug as partition key — same slug always goes to same partition
        // ensures ordering for the same URL
        CompletableFuture<SendResult<String, QrGenerationEvent>> future =
                kafkaTemplate.send(topic, slug, event);

        future.whenComplete((result, ex) -> {
            if (ex != null) {
                log.error(
                        "Failed to publish QR event: shortUrlId={} slug={} error={}",
                        shortUrlId, slug, ex.getMessage());
            } else {
                log.debug(
                        "QR event published: shortUrlId={} slug={} partition={} offset={}",
                        shortUrlId, slug,
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset()
                );
            }
        });
    }
}