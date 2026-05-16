package com.dishari.in.web.dto.response;

import com.dishari.in.domain.entity.BioLink;
import java.time.Instant;
import java.util.UUID;

public record BioLinkResponse(
        UUID    id,
        String  title,
        String  url,
        String  iconType,
        int     position,
        long    clickCount,
        boolean isActive,
        boolean hasAdvancedConfig,
        Instant createdAt
) {
    public static BioLinkResponse from(BioLink link) {
        return new BioLinkResponse(
                link.getId(),
                link.getTitle(),
                link.getUrl(),
                link.getIconType(),
                link.getPosition(),
                link.getClickCount(),
                link.isActive(),
                link.isHasAdvancedConfig(),
                link.getCreatedAt()
        );
    }
}