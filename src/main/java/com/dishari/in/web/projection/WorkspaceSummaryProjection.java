package com.dishari.in.web.projection;

import com.dishari.in.domain.enums.Plan;
import com.dishari.in.domain.enums.WorkspaceMemberRole;

import java.time.Instant;
import java.util.UUID;

public interface WorkspaceSummaryProjection {
    UUID getId();
    String getName();
    String getSlug();
    String getLogoUrl();
    Plan getPlan();
    boolean isPersonal();
    WorkspaceMemberRole getCurrentUserRole();
    Integer getMemberCount();
    Long getLinkCount();
    Instant getCreatedAt();
}
