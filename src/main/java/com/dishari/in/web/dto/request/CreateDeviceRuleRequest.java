package com.dishari.in.web.dto.request;

import com.dishari.in.domain.enums.DeviceType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record CreateDeviceRuleRequest(
        @NotNull(message = "Device type is required")
        DeviceType deviceType ,

        @NotNull(message = "Destination URL is required")
        @Pattern(regexp = "^(https?://|www\\.)[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}(/.*)?$",
                message = "Invalid URL format")

        String destinationUrl ,

        @NotNull(message = "Is default is required")
        boolean isDefault
) {}
