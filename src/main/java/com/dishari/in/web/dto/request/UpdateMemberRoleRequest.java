package com.dishari.in.web.dto.request;

import com.dishari.in.domain.enums.WorkspaceMemberRole;
import jakarta.validation.constraints.NotNull;

public record UpdateMemberRoleRequest(
        @NotNull(message = "Role is required")
        WorkspaceMemberRole role
) {}