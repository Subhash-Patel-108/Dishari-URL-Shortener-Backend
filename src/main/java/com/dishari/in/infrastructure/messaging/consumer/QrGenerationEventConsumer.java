package com.dishari.in.infrastructure.messaging.consumer;

import com.dishari.in.domain.entity.ShortUrl;
import com.dishari.in.domain.repository.ShortUrlRepository;
import com.dishari.in.infrastructure.messaging.event.QrGenerationEvent;
import com.dishari.in.infrastructure.qr.QrCodeGeneratorService;
import com.dishari.in.infrastructure.qr.QrCodeStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.BackOff;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.kafka.retrytopic.TopicSuffixingStrategy;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class QrGenerationEventConsumer {

    private final QrCodeGeneratorService qrCodeGeneratorService;
    private final QrCodeStorageService qrCodeStorageService;
    private final ShortUrlRepository shortUrlRepository;

    @RetryableTopic(
            attempts = "3",
            backOff = @BackOff(delay = 1000, multiplier = 3.0),
            topicSuffixingStrategy = TopicSuffixingStrategy.SUFFIX_WITH_INDEX_VALUE,
            dltTopicSuffix = ".DLT",
            autoCreateTopics = "true"
    )
    @KafkaListener(
            topics = "qr-generation",
            groupId = "qr-generation-group",
            containerFactory = "qrKafkaListenerContainerFactory"
    )
    public void consume(@Payload QrGenerationEvent event,
                        @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
                        @Header(KafkaHeaders.OFFSET) long offset) {

        log.info("Processing QR generation event: shortUrlId={}, slug={}, partition={}, offset={}",
                event.getShortUrlId(), event.getSlug(), partition, offset);

        try {
            processQrGeneration(event);
        } catch (Exception ex) {
            log.error("Failed to process QR generation for slug={}", event.getSlug(), ex);
            throw ex; // Let Spring Kafka retry mechanism handle it
        }
    }

    /**
     * Core business logic - separated for better testability and readability
     */
    private void processQrGeneration(QrGenerationEvent event) {

        // Guard: Check if ShortUrl still exists
        Optional<ShortUrl> optionalUrl = shortUrlRepository
                .findBySlugAndDeletedAtIsNull(event.getSlug());

        if (optionalUrl.isEmpty()) {
            log.warn("ShortUrl no longer exists or was deleted. Skipping QR generation: slug={}",
                    event.getSlug());
            return;
        }

        ShortUrl shortUrl = optionalUrl.get();

        // Delete old QR file if exists (cleanup)
        if (shortUrl.getQrCodeUrl() != null) {
            try {
                qrCodeStorageService.deleteFile(shortUrl.getQrCodeUrl());
                log.debug("Old QR file deleted successfully for slug={}", event.getSlug());
            } catch (Exception ex) {
                log.warn("Failed to delete old QR file for slug={}", event.getSlug(), ex);
                // Continue anyway - don't fail the whole process
            }
        }

        // Generate new QR code
        QrCodeGeneratorService.QrCodeGeneratedResult result =
                qrCodeGeneratorService.generateAndSave(
                        event.getShortUrlString(),
                        event.getSlug(),
                        event.getSize(),
                        event.getFgColor(),
                        event.getBgColor(),
                        event.getLogoUrl(),
                        event.getFormat()
                );

        // Update database (this should be in its own small transaction)
        updateQrCodeUrlInDatabase(event.getShortUrlId(), result.fileUrl(), event.getSlug());
    }

    @Transactional
    protected void updateQrCodeUrlInDatabase(UUID shortUrlId, String fileUrl, String slug) {
        int updatedRows = shortUrlRepository.updateQrCodeUrl(shortUrlId, fileUrl);

        if (updatedRows == 0) {
            log.warn("No rows updated for QR code URL. shortUrlId={}", shortUrlId);
        } else {
            log.info("Successfully updated QR code URL: slug={}, fileUrl={}", slug, fileUrl);
        }
    }

    // ── Dead Letter Topic Handler ─────────────────────────────────────
    @KafkaListener(
            topics = "qr-generation.DLT",
            groupId = "qr-generation-dlt-group"
    )
    public void handleDeadLetter(@Payload QrGenerationEvent event,
                                 @Header(KafkaHeaders.RECEIVED_TOPIC) String originalTopic) {

        log.error("QR Generation permanently failed and moved to DLT. " +
                        "shortUrlId={}, slug={}, originalTopic={}",
                event.getShortUrlId(), event.getSlug(), originalTopic);

        // TODO: Implement proper failure handling
        // Options:
        // 1. Notify admin via email/Slack
        // 2. Update ShortUrl status to QR_GENERATION_FAILED
        // 3. Store in failed_events table for manual retry dashboard
    }
}