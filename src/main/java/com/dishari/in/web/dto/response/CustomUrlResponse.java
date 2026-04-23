package com.dishari.in.web.dto.response;

import com.dishari.in.domain.entity.*;
import com.dishari.in.domain.enums.UrlStatus;
import com.dishari.in.web.dto.request.TagRequest;

import java.time.Instant;
import java.util.Set;
import java.util.stream.Collectors;

public record CustomUrlResponse(
        String id ,
        String shortUrl , // https://<baseUrl>/<slug>
        String slug , // <slug>
        String originalUrl ,
        String title ,
        Instant expiresAt ,
        Long maxClick ,
        Long clickCount ,
        String qrCodeUrl ,
        UrlUtmResponse utm ,
        boolean hasGeoRule ,
        boolean hasDeviceRule ,
        boolean hasLinkRotation ,
        boolean hasTags ,
        Set<TagResponse> tags ,
        Set<GeoRuleResponse> geoRules ,
        Set<DeviceRuleResponse> deviceRules ,
        LinkRotationResponse linkRotation ,
        UrlStatus status ,
        boolean flagged
) {
    public static CustomUrlResponse fromEntity(ShortUrl shortUrl , Set<Tag> tags , Set<GeoRule> geoRules , Set<DeviceRule> deviceRules , LinkRotation linkRotation , String baseUrl) {
        String fullShortUrl = (baseUrl != null ? baseUrl : "https://dishari.in")
                + "/" + shortUrl.getSlug();

        return new CustomUrlResponse(
                shortUrl.getId().toString() ,
                fullShortUrl ,
                shortUrl.getSlug() ,
                shortUrl.getOriginalUrl() ,
                shortUrl.getTitle() ,
                shortUrl.getExpiresAt() ,
                shortUrl.getMaxClicks() ,
                shortUrl.getClickCount() ,
                shortUrl.getQrCodeUrl() ,
                UrlUtmResponse.fromEntity(shortUrl.getUtmSource() , shortUrl.getUtmMedium() , shortUrl.getUtmCampaign() , shortUrl.getUtmTerm() , shortUrl.getUtmContent()),
                shortUrl.isHasGeoRule() ,
                shortUrl.isHasDeviceRule() ,
                shortUrl.isHasLinkRotation() ,
                tags != null && !tags.isEmpty() ,
                tags != null ? tags.stream().map(TagResponse::fromEntity).collect(Collectors.toSet()) : Set.of() ,
                geoRules != null ? geoRules.stream().map(GeoRuleResponse::fromEntity).collect(Collectors.toSet()) : Set.of() ,
                deviceRules != null ? deviceRules.stream().map(DeviceRuleResponse::fromEntity).collect(Collectors.toSet()) : Set.of() ,
                linkRotation != null ? LinkRotationResponse.fromEntity(linkRotation) : null ,
                shortUrl.getStatus() ,
                shortUrl.isFlagged()
        );
    }
}
