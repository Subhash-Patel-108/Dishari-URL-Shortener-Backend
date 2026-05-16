package com.dishari.in.web.dto.response;

import com.dishari.in.domain.entity.BioPage;

import java.util.List;

public record BioPagePublicResponse(
        String handle,
        String displayName,
        String bio,
        String avatarUrl,
        List<BioLinkResponse> links
) {
    public static BioPagePublicResponse from(BioPage page) {
        return new BioPagePublicResponse(
                page.getHandle(),
                page.getDisplayName(),
                page.getBio(),
                page.getAvatarUrl(),
                page.getLinks().stream()
                        .filter(l -> l.isActive()
                                && l.getDeletedAt() == null)
                        .map(BioLinkResponse::from)
                        .toList()
        );
    }
}