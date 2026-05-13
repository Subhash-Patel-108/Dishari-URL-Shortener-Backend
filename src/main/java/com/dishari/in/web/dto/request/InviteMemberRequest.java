package com.dishari.in.web.dto.request;

import com.dishari.in.domain.enums.WorkspaceMemberRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record InviteMemberRequest(
        @NotBlank(message = "Email is required")
        @Email(message = "Invalid email format")
        String email,

        @NotNull(message = "Role is required")
        /*
         *     OWNER,
         *     ADMIN,
         *     EDITOR ,
         *     VIEWER
         */
        WorkspaceMemberRole role
) {}
