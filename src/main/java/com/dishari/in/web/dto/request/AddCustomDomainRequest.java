package com.dishari.in.web.dto.request;

import com.dishari.in.domain.enums.VerificationType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record AddCustomDomainRequest(
        @NotBlank(message = "Domain is required")
        @Pattern(regexp = "^([a-zA-Z0-9]([a-zA-Z0-9-]*[a-zA-Z0-9])?\\.)+[a-zA-Z]{2,}$",
                message = "Invalid domain format")
        String domain,

        String rootRedirect,

        String errorRedirect,

        VerificationType verificationType
) {}