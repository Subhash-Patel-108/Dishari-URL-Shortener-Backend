package com.dishari.in.web.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateWorkspaceRequest(
        @NotBlank(message = "Workspace name is required")
        @Size(min = 2, max = 100, message = "Workspace name must be between 2 and 100 characters")
        String name,

        @NotBlank(message = "Slug is required")
        @Pattern(regexp = "^[a-z0-9-]+$", message = "Slug can only contain lowercase letters, numbers, and hyphens")
        @Size(min = 3, max = 50, message = "Slug must be between 3 and 50 characters")
        String slug,

        @Size(max = 500, message = "Description cannot exceed 500 characters")
        String description,

        @Pattern(regexp = "^#([A-Fa-f0-9]{6}|[A-Fa-f0-9]{3})$|^$", message = "Brand color must be a valid hex code")
        String brandColor ,

        Boolean personal
) {}
