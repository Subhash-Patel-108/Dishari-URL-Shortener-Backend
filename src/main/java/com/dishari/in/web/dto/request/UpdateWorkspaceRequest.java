package com.dishari.in.web.dto.request;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateWorkspaceRequest(
        @Size(min = 2, max = 100, message = "Workspace name must be between 2 and 100 characters")
        String name,

        @Size(max = 500, message = "Description cannot exceed 500 characters")
        String description,

        @Pattern(regexp = "^(https?://).+|^$", message = "Logo URL must be a valid URL")
        String logoUrl,

        @Pattern(regexp = "^#([A-Fa-f0-9]{6}|[A-Fa-f0-9]{3})$|^$", message = "Brand color must be a valid hex code")
        String brandColor
) {}
