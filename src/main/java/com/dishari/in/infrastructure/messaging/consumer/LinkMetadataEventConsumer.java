package com.dishari.in.infrastructure.messaging.consumer;

import com.dishari.in.domain.entity.LinkMetadata;
import com.dishari.in.domain.repository.LinkMetadataRepository;
import com.dishari.in.infrastructure.messaging.event.LinkMetadataEvent;
import com.dishari.in.infrastructure.metadata.MetadataScraperService;
import com.dishari.in.utils.DomainUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Slf4j
@RequiredArgsConstructor
@Component // Ensure this is marked as a Component for Spring to find the KafkaListener
public class LinkMetadataEventConsumer {

    private final MetadataScraperService scraperService;
    private final LinkMetadataRepository repository;

    @KafkaListener(
            topics = "${app.kafka.topics.link-metadata-generation}",
            groupId = "${app.kafka.groups.link-metadata-generation}",
            containerFactory = "linkMetadataEventKafkaListenerContainerFactory"
    )
    public void consume(@Payload LinkMetadataEvent event,
                        @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
                        @Header(KafkaHeaders.OFFSET) long offset) {

        log.info("Consuming Link Metadata Event | Partition: {} | Offset: {}", partition, offset);

        try {
            processMetadata(event);
        } catch (Exception ex) {
            // SDE-3: We catch everything to prevent the Kafka partition from stuck-retrying
            // a "poison pill" message that will never succeed.
            log.error("Fatal error processing metadata for URL: {}", event.getOriginalUrl(), ex);
        }
    }

    private void processMetadata(LinkMetadataEvent event) {
        String url = event.getOriginalUrl();
        String hash = DigestUtils.sha256Hex(url);
        String rootDomain = DomainUtil.extractRootDomain(url);

        // 1. Quick lookup to avoid unnecessary network I/O
        if (repository.existsByUrlHash(hash)) {
            log.debug("Metadata already cached for hash: {}", hash);
            return;
        }

        // 2. Perform the scrape
        scraperService.scrape(url).ifPresentOrElse(
                metadata -> {
                    metadata.setUrlHash(hash);
                    saveMetadata(metadata);
                },
                () -> handleScrapeFailure(hash, rootDomain)
        );
    }

    private void saveMetadata(LinkMetadata metadata) {
        try {
            repository.save(metadata);
            log.info("Metadata successfully persisted for domain: {}", metadata.getSiteName());
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            // Handles the case where another consumer instance saved the hash
            // while this one was busy scraping.
            log.warn("Metadata hash collision for {}. Row already exists.", metadata.getUrlHash());
        }
    }

    private void handleScrapeFailure(String hash, String rootDomain) {
        log.warn("Scraping failed for domain: {}. Creating placeholder.", rootDomain);
        LinkMetadata placeholder = LinkMetadata.builder()
                .urlHash(hash)
                .title("Link Preview Unavailable")
                .description("No preview could be generated for this destination.")
                .lastScrapedAt(Instant.now())
                .build();
        repository.save(placeholder);
    }
}
