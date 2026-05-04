package com.dishari.in.web.dto.response;

import com.dishari.in.domain.entity.ShortUrl;

import java.time.Instant;

public record ShortUrlUpdateResponse(
        String id ,
        String originalUrl ,
        String title ,
        UrlUtmResponse utm ,
        Instant expiresAt ,
        Long maxClicks
) {

    public static ShortUrlUpdateResponse fromEntity(ShortUrl shortUrl) {
        return new ShortUrlUpdateResponse(
                shortUrl.getId().toString() ,
                shortUrl.getOriginalUrl() ,
                shortUrl.getTitle() ,
                UrlUtmResponse.fromEntity(shortUrl.getUtmSource() , shortUrl.getUtmMedium() , shortUrl.getUtmCampaign() , shortUrl.getUtmTerm() , shortUrl.getUtmContent()) ,
                shortUrl.getExpiresAt() ,
                shortUrl.getMaxClicks()
        );
    }
}
