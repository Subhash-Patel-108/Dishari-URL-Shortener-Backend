package com.dishari.in.web.dto.response;

import com.dishari.in.domain.entity.LinkMetadata;

public record LinkMetadataResponse(
        String title,
        String description,
        String image,
        String favicon,
        String siteName ,
        String author,
        String canonicalUrl,
        String destinationType,
        String contentType         // article, video, website...
) {

    public static LinkMetadataResponse fromEntity(LinkMetadata linkMetadata) {
        return new LinkMetadataResponse(
                linkMetadata.getTitle() ,
                linkMetadata.getDescription() ,
                linkMetadata.getImageUrl() ,
                linkMetadata.getFaviconUrl() ,
                linkMetadata.getSiteName() ,
                linkMetadata.getAuthor() ,
                linkMetadata.getCanonicalUrl() ,
                linkMetadata.getDestinationType(),
                linkMetadata.getContentType()
        );
    }
}
