package com.dishari.in.web.dto.response;

import com.dishari.in.domain.entity.BioPage;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record BioPageResponse(
        UUID id,
        String handle,
        String displayName,
        String bio,
        String avatarUrl,
        long viewCount,
        boolean isActive,
        List<BioLinkResponse> links,
        Instant createdAt,
        Instant updatedAt
) {
    public static BioPageResponse from(BioPage page) {
        return new BioPageResponse(
                page.getId(),
                page.getHandle(),
                page.getDisplayName(),
                page.getBio(),
                page.getAvatarUrl(),
                page.getViewCount(),
                Boolean.TRUE.equals(page.getIsActive()),
                page.getLinks().stream()
                        .filter(l -> l.getDeletedAt() == null)
                        .map(BioLinkResponse::from)
                        .toList(),
                page.getCreatedAt(),
                page.getUpdatedAt()
        );
    }
}