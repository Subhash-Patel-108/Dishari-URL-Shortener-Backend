package com.dishari.in.web.dto.response;

import com.dishari.in.domain.entity.ShortUrl;
import com.dishari.in.domain.enums.UrlStatus;

import java.time.Instant;

public record UpdateUrlStatusResponse(
        String id ,
        UrlStatus status ,
        Instant updatedAt
) {

    public static UpdateUrlStatusResponse fromEntity(ShortUrl shortUrl) {
        return new UpdateUrlStatusResponse(
                shortUrl.getId().toString(),
                shortUrl.getStatus(),
                shortUrl.getUpdatedAt()
        );
    }
}
