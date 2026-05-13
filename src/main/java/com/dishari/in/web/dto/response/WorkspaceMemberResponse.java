package com.dishari.in.web.dto.response;

import com.dishari.in.domain.enums.WorkspaceMemberRole;
import com.dishari.in.domain.enums.WorkspaceMemberStatus;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;

public record WorkspaceMemberResponse(
        String id,
        UserInfo user,
        WorkspaceMemberRole role,
        WorkspaceMemberStatus status,
        InvitedByInfo invitedBy,
        Instant invitedAt,
        Instant joinedAt,
        Instant removedAt
) {
    public record UserInfo(
            String id,
            String name,
            String email,
            String avatarUrl
    ) {}

    public record InvitedByInfo(
            String id,
            String name,
            String email
    ) {}
}