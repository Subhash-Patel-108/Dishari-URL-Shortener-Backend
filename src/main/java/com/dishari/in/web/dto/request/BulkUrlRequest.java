package com.dishari.in.web.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public record BulkUrlRequest (
        @NotNull(message = "Original Url is required")
        @Pattern(regexp = "^(https?://|www\\.)[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}(/.*)?$", message = "Invalid URL format")
        String originalUrl,

        String title,
        UrlUtmRequest utm,
        Instant expiresAt,
        Long maxClicks,

        @Size(max = 128, message = "Password must be less than 128 characters")
        String password
){
}
