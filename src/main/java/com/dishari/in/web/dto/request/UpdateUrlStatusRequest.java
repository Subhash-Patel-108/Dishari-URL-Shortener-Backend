package com.dishari.in.web.dto.request;

import com.dishari.in.domain.enums.UrlStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateUrlStatusRequest(
        @NotNull(message = "Status cannot be null")
        UrlStatus status
) {
}
