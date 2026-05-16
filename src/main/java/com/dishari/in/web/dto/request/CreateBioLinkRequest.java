package com.dishari.in.web.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateBioLinkRequest(

        @NotBlank(message = "Title is required")
        @Size(max = 100, message = "Title max 100 characters")
        String title,

        @NotBlank(message = "URL is required")
        @Pattern(regexp = "^(https?://|www\\.)[a-zA-Z0-9.-]+" + "\\.[a-zA-Z]{2,}(/.*)?$", message = "Invalid URL format")
        @Size(max = 2048)
        String url,

        String iconType,    // e.g. "instagram", "twitter", "link"

        Integer position    // null = append to end
) {}
