package com.dishari.in.web.dto.response;

import com.dishari.in.domain.entity.ShortUrl;
import com.dishari.in.domain.enums.UrlStatus;

import java.time.Instant;
import java.util.UUID;

public record NormalUrlResponse(
        UUID id,
        String shortUrl,                 // full link e.g. https://dishari.in/abc123
        String slug,
        String originalUrl,
        String title,

        // UTM parameters that were saved
        UrlUtmResponse utm ,

        Instant createdAt,
        Instant expiresAt,               // ← this is the exact date/time when URL becomes invalid
        Long maxClicks,
        String qrCodeUrl,
        boolean isPasswordProtected,
        boolean isActive ,
        boolean isAlreadyCreated
) {

    /**
     * Static factory method - The ONLY recommended way to create this response.
     *
     * @param shortUrl the persisted entity
     * @param baseUrl  the base domain (e.g. "https://dishari.in" or your custom domain)
     * @return immutable NormalUrlResponse
     */
    public static NormalUrlResponse fromEntity(ShortUrl shortUrl, String baseUrl , Boolean isAlreadyCreated) {

        // Defensive null checks + clean full short URL construction
        String fullShortUrl = (baseUrl != null ? baseUrl : "https://dishari.in")
                + "/" + shortUrl.getSlug();

        return new NormalUrlResponse(
                shortUrl.getId(),
                fullShortUrl,
                shortUrl.getSlug(),
                shortUrl.getOriginalUrl(),
                shortUrl.getTitle(),
                UrlUtmResponse.fromEntity(
                        shortUrl.getUtmSource(),
                        shortUrl.getUtmMedium(),
                        shortUrl.getUtmCampaign(),
                        shortUrl.getUtmTerm(),
                        shortUrl.getUtmContent()
                ),
                shortUrl.getCreatedAt(),
                shortUrl.getExpiresAt(),
                shortUrl.getMaxClicks(),
                shortUrl.getQrCodeUrl(),
                shortUrl.getHashedPassword() != null,           // password protected?
                shortUrl.getStatus() == UrlStatus.ACTIVE,        // still active?
                isAlreadyCreated
        );
    }
}
