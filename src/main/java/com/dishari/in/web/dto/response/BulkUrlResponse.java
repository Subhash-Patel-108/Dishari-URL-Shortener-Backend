package com.dishari.in.web.dto.response;

import com.dishari.in.domain.entity.ShortUrl;
import com.dishari.in.domain.enums.UrlStatus;

import java.time.Instant;

public record BulkUrlResponse(
        String id ,
        String shortUrl,                 // full link e.g. https://dishari.in/abc123
        String slug,
        String originalUrl,
        String title,

        // UTM parameters that were saved
        UrlUtmResponse utm ,

        Instant createdAt,
        Instant expiresAt,               // ← this is the exact date/time when URL becomes invalid
        Long maxClicks,
        boolean isPasswordProtected,
        boolean isActive ,
        boolean isAlreadyCreated
) {
    public static BulkUrlResponse fromEntity(ShortUrl shortUrl , String baseUrl , boolean isAlreadyCreated) {
        String completeShortUrl = baseUrl + "/" + shortUrl.getSlug() ;
        return new BulkUrlResponse(
                shortUrl.getId().toString() ,
                completeShortUrl ,
                shortUrl.getSlug() ,
                shortUrl.getOriginalUrl() ,
                shortUrl.getTitle() ,
                UrlUtmResponse.fromEntity(shortUrl.getUtmSource() , shortUrl.getUtmMedium() , shortUrl.getUtmCampaign() , shortUrl.getUtmContent() , shortUrl.getUtmTerm()) ,
                shortUrl.getCreatedAt() ,
                shortUrl.getExpiresAt() ,
                shortUrl.getMaxClicks() ,
                shortUrl.getHashedPassword() != null ,
                shortUrl.getStatus() == UrlStatus.ACTIVE ,
                isAlreadyCreated
        );
    }
}
