package com.dishari.in.web.dto.request;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateBioLinkRequest(

        @Size(max = 100)
        String title,

        @Pattern(regexp = "^(https?://|www\\.)[a-zA-Z0-9.-]+" +
                "\\.[a-zA-Z]{2,}(/.*)?$",
                message = "Invalid URL format")
        String url,

        String iconType,

        Boolean isActive
) {}