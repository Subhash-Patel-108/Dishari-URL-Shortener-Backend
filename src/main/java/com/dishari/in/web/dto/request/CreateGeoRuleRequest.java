package com.dishari.in.web.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateGeoRuleRequest(
        @Size(min = 2 , max = 2 , message = "Country code must be 2 characters long")
        @NotBlank(message = "Country code is required")
        String countryCode ,

        @NotBlank(message = "Destination URL is required")
        @Pattern(regexp = "^(https?://|www\\.)[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}(/.*)?$",
                message = "Invalid URL format")
        String  destinationUrl ,

        @NotNull(message = "Priority is required")
        Integer priority ,

        @NotNull(message = "Is default is required")
        boolean isDefault
) {
}
