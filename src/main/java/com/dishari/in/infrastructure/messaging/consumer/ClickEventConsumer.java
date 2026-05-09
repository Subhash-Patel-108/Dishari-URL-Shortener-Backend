package com.dishari.in.infrastructure.messaging.consumer;

import com.dishari.in.domain.entity.ClickEvent;
import com.dishari.in.domain.entity.ShortUrl;
import com.dishari.in.domain.enums.DeviceType;
import com.dishari.in.domain.repository.ClickEventRepository;
import com.dishari.in.domain.repository.ShortUrlRepository;
import com.dishari.in.exception.UUIDParsingException;
import com.dishari.in.exception.UrlNotFoundException;
import com.dishari.in.infrastructure.geoip.GeoIpService;
import com.dishari.in.infrastructure.messaging.event.ClickEventGenerationEvent;
import lombok.RequiredArgsConstructor;
import lombok.Synchronized;
import lombok.extern.slf4j.Slf4j;
import nl.basjes.parse.useragent.UserAgent;
import nl.basjes.parse.useragent.UserAgentAnalyzer;
import org.hibernate.boot.models.annotations.internal.SynchronizeAnnotation;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class ClickEventConsumer {

    private final ShortUrlRepository shortUrlRepository ;
    private final ClickEventRepository clickEventRepository ;
    private final GeoIpService geoIpService ;
    private final UserAgentAnalyzer userAgentAnalyzer;
    private final RedissonClient redissonClient;

    @Value("${app.pepper.ip-pepper}")
    private String ipPepper;

    @KafkaListener(
            topics = "${app.kafka.topics.click-event-generation}" ,
            groupId = "${app.kafka.groups.click-event-generation}",
            containerFactory = "clickEventKafkaListenerContainerFactory"
    )
    public void consume(@Payload ClickEventGenerationEvent event , @Header(KafkaHeaders.RECEIVED_PARTITION) int partition, @Header(KafkaHeaders.OFFSET) long offset) {
        log.info("Consuming message from partition: {} and offset: {}", partition, offset);

        try {
            createClickEvent(event);
            log.info("Click Event Consumer :: shortUrlId : {} " , event.getShortUrlId());
        } catch (Exception ex) {
            log.error("Failed to create click event for shortUrlId={}", event.getShortUrlId(), ex);
        }
    }

    //Helper method that generate the click event and store it into the database
    private void createClickEvent(ClickEventGenerationEvent event) {
        UUID shortUrlId = parseToUUID(event.getShortUrlId() ) ;
        ShortUrl shortUrl = shortUrlRepository.findByIdAndDeletedAtIsNull(shortUrlId)
                .orElseThrow(() -> new UrlNotFoundException("Url not found with Id : " + shortUrlId));

        shortUrl.setClickCount(shortUrl.getClickCount() + 1);

        String rawIp = event.getIpAddress() ;
        GeoIpService.GeoLocation geoLocation = geoIpService.getLocation(rawIp) ;

        String rawIpWithPepper = addPepper(rawIp) ;
        String hashedIpAddress = DigestUtils.sha256Hex(rawIpWithPepper) ;

        ClickEvent clickEvent = ClickEvent.builder()
                .shortUrl(shortUrl)
                .ipHash(hashedIpAddress)
                .userAgent(event.getUserAgent() == null ? "Unknown" : event.getUserAgent())
                .country(geoLocation.countryCode() == null ? "NA" : geoLocation.countryCode())
                .countryName(geoLocation.countryName() == null ? "Unknown" : geoLocation.countryName())
                .city(geoLocation.city() == null ? "Unknown" : geoLocation.city())
                .refererDomain(event.getReferer())
                .variantId(event.getVariantId())
                .clickedAt(event.getOccurredAt())
                .unique(isUniqueClick(shortUrlId , hashedIpAddress))
                .build() ;

        enrichUserAgent(clickEvent , clickEvent.getUserAgent());

        clickEventRepository.save(clickEvent) ;
        shortUrlRepository.save(shortUrl);
        return ;
    }

    private UUID parseToUUID(String shortUrlId) {
        try {
            return UUID.fromString(shortUrlId) ;
        }catch (Exception ex){
            log.error("ClickEventConsumer::parseToUUID()::Failed to parse shortUrlId={}", shortUrlId, ex);
            throw new UUIDParsingException("Invalid short url id. Failed to parse id.") ;
        }
    }

    private String  addPepper(String ip) {
        return ip + ipPepper;
    }

    private void enrichUserAgent(ClickEvent builder, String uaString) {
        // The analyzer is thread-safe, so multiple Kafka threads can use it
        UserAgent parsedUa = userAgentAnalyzer.parse(uaString);

        builder.setBrowser(parsedUa.getValue("AgentName"));
        builder.setOs(parsedUa.getValue("OperatingSystemName"));

        // Map the DeviceClass to your Enum
        String deviceClass = parsedUa.getValue("DeviceClass");
        builder.setDevice(mapToDeviceType(deviceClass));
    }

    private DeviceType mapToDeviceType(String deviceClass) {
        return switch (deviceClass) {
            case "Phone" -> DeviceType.MOBILE;
            case "Tablet" -> DeviceType.TABLET;
            default -> DeviceType.DESKTOP;
        };
    }

    private boolean isUniqueClick(UUID shortUrlId, String hashedIp) {
        String bloomKey = "bloom:unique_clicks:" + shortUrlId;
        RBloomFilter<String> bloomFilter = redissonClient.getBloomFilter(bloomKey);

        // If the filter is new, initialize it (e.g., 100k expected insertions, 3% error rate)
        if (!bloomFilter.isExists()) {
            bloomFilter.tryInit(100000L, 0.03);
        }

        // Returns true if the IP was added for the first time
        return bloomFilter.add(hashedIp);
    }
}
