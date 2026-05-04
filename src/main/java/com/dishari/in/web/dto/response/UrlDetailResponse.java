package com.dishari.in.web.dto.response;

import com.dishari.in.domain.entity.DeviceRule;
import com.dishari.in.domain.entity.GeoRule;
import com.dishari.in.domain.entity.LinkRotation;
import com.dishari.in.domain.entity.ShortUrl;
import com.dishari.in.domain.enums.UrlStatus;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public record UrlDetailResponse(
        UUID id ,
        String shortUrl ,
        String slug ,

        String originalUrl ,
        String title ,

        UrlUtmResponse utm ,

        Instant createdAt ,
        Instant expiresAt ,
        Long clickCount ,
        Long maxClick ,
        String qrCodeUrl ,

        boolean isPasswordProtected ,
        boolean isActive ,

        boolean hasGeoRule,
        boolean hasDeviceRule ,
        boolean hasLinkRotation ,
        boolean hasTags ,

        Set<GeoRuleResponse> geoRule ,
        Set<DeviceRuleResponse> deviceRule ,
        LinkRotationResponse linkRotation ,
        List<TagResponse> tags
) {
    public static UrlDetailResponse fromEntity(ShortUrl shortUrl, String baseUrl, List<GeoRule> geoRules , List<DeviceRule> deviceRules , LinkRotation linkRotation) {

        String fullShortUrl = baseUrl + "/" + shortUrl.getSlug();

        return new UrlDetailResponse(
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
                shortUrl.getClickCount(),
                shortUrl.getMaxClicks(),
                shortUrl.getQrCodeUrl(),
                shortUrl.getHashedPassword() != null,
                shortUrl.getStatus() == UrlStatus.ACTIVE,

                shortUrl.isHasGeoRule(),
                shortUrl.isHasDeviceRule(),
                shortUrl.isHasLinkRotation() ,
                !shortUrl.getTags().isEmpty() ,
                geoRules != null ? geoRules.stream().map(GeoRuleResponse::fromEntity).collect(Collectors.toSet()) : Set.of() ,
                deviceRules != null ? deviceRules.stream().map(DeviceRuleResponse::fromEntity).collect(Collectors.toSet()) : Set.of() ,
                linkRotation != null ? LinkRotationResponse.fromEntity(linkRotation) : null ,
                shortUrl.getTags().stream().map(TagResponse::fromEntity).toList()
        );
    }
}
