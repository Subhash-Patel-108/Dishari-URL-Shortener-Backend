package com.dishari.in.web.dto.response;

import com.dishari.in.domain.entity.Workspace;
import com.dishari.in.domain.enums.Plan;
import com.dishari.in.domain.enums.WorkspaceMemberRole;

import java.time.Instant;
import java.util.List;

public record WorkspaceSummary(
        String id,
        String name,
        String slug,
        String logoUrl,
        Plan plan,
        boolean personal,
        WorkspaceMemberRole currentUserRole,
        int memberCount,
        int linkCount,
        Instant createdAt
) {
}
