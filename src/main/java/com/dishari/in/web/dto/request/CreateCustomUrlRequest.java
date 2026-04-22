package com.dishari.in.web.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.Set;

public record CreateCustomUrlRequest(
        @NotNull(message = "Original Url is required")
        @Pattern(regexp = "^(https?://|www\\.)[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}(/.*)?$",
                message = "Invalid URL format")
        String originalUrl ,

        @NotNull(message = "Custom Url is required")
        @Size(min = 3, max = 32, message = "Slug must be between 3 and 32 characters")
        //@Pattern(regexp = "^[a-zA-Z0-9_]+$", message = "Slug can only contain letters, numbers, and underscores")
        String customSlug ,

        String title ,

        UrlUtmRequest utm , // null = no utm
        Instant expiresAt, // null = no expiration
        Long maxClicks , // null = no limit

        @Size(max = 128 , message = "Password must be less than 128 characters")
        String password , // null = no password

        //Custom features like hasGeoRule , hasDeviceRule , hasLinkRotation , tags

        Set<TagRequest> tags ,

        Set<CreateGeoRuleRequest> geoRules ,

        Set<CreateDeviceRuleRequest> deviceRules

        //TODO: implementing Link Rotation

) {
}
