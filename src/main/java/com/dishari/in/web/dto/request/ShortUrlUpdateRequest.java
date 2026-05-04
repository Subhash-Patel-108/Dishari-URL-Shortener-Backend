package com.dishari.in.web.dto.request;

import jakarta.annotation.Nullable;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public record ShortUrlUpdateRequest(
        @Pattern(regexp = "^(https?://|www\\.)[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}(/.*)?$",
                message = "Invalid URL format")
        @Nullable
        String originalUrl ,

        String title ,

        UrlUtmRequest utm ,

        @Future(message = "The expiry date must be in the future.")
        Instant expiresAt,

        Long maxClicks ,

        @Size(max = 128 , message = "Password must be less than 128 characters")
        String password
) {
}
