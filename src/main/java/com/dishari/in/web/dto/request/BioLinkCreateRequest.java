package com.dishari.in.web.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record BioLinkCreateRequest(
        @NotBlank(message = "Title is required")
        @Size(max = 100, message = "Title cannot exceed 100 characters")
        String title,

        @NotBlank(message = "URL is required")
        @Size(max = 2048, message = "URL cannot exceed 2048 characters")
        @Pattern(regexp = "^(http|https)://.*$", message = "URL must be a valid HTTP/HTTPS URL")
        String url,

        @Size(max = 50, message = "Icon type cannot exceed 50 characters")
        String iconType,

        Integer position,

        Boolean isActive,

        Boolean hasAdvancedConfig
) {
    public BioLinkCreateRequest {
        if (iconType == null) {
            iconType = "link";
        }
        if (isActive == null) {
            isActive = true;
        }
        if (hasAdvancedConfig == null) {
            hasAdvancedConfig = false;
        }
    }
}