package com.dishari.in.application.serviceImpl;

import com.dishari.in.domain.entity.ShortUrl;
import com.dishari.in.domain.entity.User;
import com.dishari.in.domain.repository.ShortUrlRepository;
import com.dishari.in.exception.UrlNotFoundException;
import com.dishari.in.exception.UnauthorizedException;
import com.dishari.in.infrastructure.messaging.producer.QrGenerationEventProducer;
import com.dishari.in.infrastructure.qr.QrCodeGeneratorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class QRCodeService {

    private final ShortUrlRepository      shortUrlRepository;
    private final QrCodeGeneratorService  qrCodeGeneratorService;
    private final QrGenerationEventProducer producer;

    @Value("${app.base-url}")
    private String baseUrl;

    // ── Async via Kafka — saves file, updates qrCodeUrl in DB ────
    // Called automatically on URL creation for PRO users
    @Transactional
    public void triggerAsyncGeneration(ShortUrl shortUrl, User principal) {
        producer.publishQrGenerationEvent(
                shortUrl.getId(),
                shortUrl.getSlug(),
                baseUrl + "/" + shortUrl.getSlug(),
                principal.getId(),
                300, "#000000", "#FFFFFF", null, "PNG"
        );
        log.info("QR async event triggered for slug={}", shortUrl.getSlug());
    }

    // ── Sync — returns raw bytes, nothing saved to disk ──────────
    // Called on GET /api/v1/urls/{id}/qr for on-demand preview
    public byte[] generateBytesOnDemand(
            UUID shortUrlId,
            int size,
            String fgColor,
            String bgColor,
            String logoUrl,
            String format,
            User principal) {

        ShortUrl shortUrl = shortUrlRepository
                .findByIdAndDeletedAtIsNull(shortUrlId)
                .orElseThrow(() -> new UrlNotFoundException(
                        "Short URL not found: " + shortUrlId));

        if (!shortUrl.getUser().getId().equals(principal.getId())) {
            throw new UnauthorizedException(
                    "You don't have permission to access this URL.");
        }

        String shortUrlString = baseUrl + "/" + shortUrl.getSlug();

        byte[] bytes = qrCodeGeneratorService.generateBytes(
                shortUrlString,
                size,
                fgColor  != null ? fgColor  : "#000000",
                bgColor  != null ? bgColor  : "#FFFFFF",
                logoUrl,
                format   != null ? format.toUpperCase() : "PNG"
        );

        log.debug("QR bytes generated on demand: slug={} size={} format={}",
                shortUrl.getSlug(), size, format);

        return bytes;
    }
}