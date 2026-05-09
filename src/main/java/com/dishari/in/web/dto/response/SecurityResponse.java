package com.dishari.in.web.dto.response;

import com.dishari.in.domain.entity.LinkMetadata;
import com.dishari.in.domain.entity.ShortUrl;

public record SecurityResponse(
        boolean isSecure,
        String rootDomain,
        boolean isFlagged
) {
    public static SecurityResponse fromEntity(ShortUrl url , LinkMetadata metadata) {
        return new SecurityResponse(
                isSecure(url.getOriginalUrl()) ,
                metadata.getSiteName(),
                url.isFlagged()
        ) ;
    }

    private static boolean isSecure(String originalUrl) {
        return originalUrl.startsWith("https://") ;
    }
}
