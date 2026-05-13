package com.dishari.in.web.dto.response;

import com.dishari.in.domain.enums.Plan;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;

public record WorkspaceResponse(
        String id,
        String name,
        String slug,
        String description,
        long linkCount ,
        Plan plan,
        boolean personal,
        boolean enabled,
        String logoUrl,
        String brandColor,
        OwnerInfo owner,
        MemberStats members,
        Instant createdAt,
        Instant updatedAt
) {
    public record OwnerInfo(
            String id,
            String name,
            String email,
            String avatarUrl
    ) {}

    public record MemberStats(
            int totalMembers,
            int activeMembers,
            int pendingInvites
    ) {}
}