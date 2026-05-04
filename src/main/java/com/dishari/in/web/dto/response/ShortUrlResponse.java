package com.dishari.in.web.dto.response;

import com.dishari.in.domain.entity.ShortUrl;
import com.dishari.in.domain.enums.UrlStatus;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ShortUrlResponse(
        UUID id,
        String slug,
        String shortUrl,
        String originalUrl,
        String title,
        long clickCount,
        UrlStatus status,
        boolean passwordProtected,
        String qrCodeUrl,
        Instant expiresAt,
        Long maxClicks,
        boolean hasGeoRule,
        boolean hasDeviceRule,
        boolean hasLinkRotation,
        UtmResponse utm,
        List<TagResponse> tags,
        Instant createdAt,
        Instant updatedAt
) {
    public static ShortUrlResponse fromEntity(ShortUrl url, String baseUrl) {
        return new ShortUrlResponse(
                url.getId(),
                url.getSlug(),
                baseUrl + "/" + url.getSlug(),
                url.getOriginalUrl(),
                url.getTitle(),
                url.getClickCount(),
                url.getStatus(),
                url.getHashedPassword() != null,
                url.getQrCodeUrl(),
                url.getExpiresAt(),
                url.getMaxClicks(),
                url.isHasGeoRule(),
                url.isHasDeviceRule(),
                url.isHasLinkRotation(),
                url.getUtmSource() != null ? new UtmResponse(
                        url.getUtmSource(),
                        url.getUtmMedium(),
                        url.getUtmCampaign(),
                        url.getUtmTerm(),
                        url.getUtmContent()
                ) : null,
                url.getTags() != null ? url.getTags().stream().map(TagResponse::fromEntity).toList() : null,
                url.getCreatedAt(),
                url.getUpdatedAt()
        );
    }
}