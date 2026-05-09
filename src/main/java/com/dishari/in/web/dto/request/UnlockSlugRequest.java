package com.dishari.in.web.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UnlockSlugRequest(
        @NotNull(message = "Password is required.")
        @Size(max = 64 , message = "Password size should not more than 64 character.")
        String password
) {
}
