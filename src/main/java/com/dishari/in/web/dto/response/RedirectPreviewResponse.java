package com.dishari.in.web.dto.response;

import com.dishari.in.domain.entity.LinkMetadata;
import com.dishari.in.domain.entity.ShortUrl;

public record RedirectPreviewResponse(
        String slug ,
        String shortUrl ,
        String destination ,
        LinkMetadataResponse metadata ,
        SecurityResponse security
) {

    public static RedirectPreviewResponse fromEntity(ShortUrl url , LinkMetadata linkMetadata , String frontendBaseUrl) {
        String shortUrl = frontendBaseUrl + "/" + url.getSlug() ;
        return new RedirectPreviewResponse(
                url.getSlug() ,
                shortUrl ,
                url.getOriginalUrl() ,
                LinkMetadataResponse.fromEntity(linkMetadata) ,
                SecurityResponse.fromEntity(url , linkMetadata)
        );
    }
}
