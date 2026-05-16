package com.dishari.in.web.dto.request;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record BioLinkUpdateRequest(
        @Size(max = 100, message = "Title cannot exceed 100 characters")
        String title,

        @Size(max = 2048, message = "URL cannot exceed 2048 characters")
        @Pattern(regexp = "^(http|https)://.*$", message = "URL must be a valid HTTP/HTTPS URL")
        String url,

        @Size(max = 50, message = "Icon type cannot exceed 50 characters")
        String iconType,

        Boolean isActive,

        Boolean hasAdvancedConfig
) {}