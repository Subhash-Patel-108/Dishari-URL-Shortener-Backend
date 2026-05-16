package com.dishari.in.web.dto.request;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record BioPageUpdateRequest(
        @Size(min = 3, max = 32, message = "Handle must be between 3 and 32 characters")
        @Pattern(regexp = "^[a-zA-Z0-9._-]+$", message = "Handle can only contain letters, numbers, dots, underscores and hyphens")
        String handle,

        @Size(max = 128, message = "Display name cannot exceed 128 characters")
        String displayName,

        @Size(max = 1024, message = "Bio cannot exceed 1024 characters")
        String bio,

        @Size(max = 1024, message = "Avatar URL cannot exceed 1024 characters")
        String avatarUrl,

        Boolean isActive
) {}