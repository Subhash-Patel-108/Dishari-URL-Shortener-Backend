package com.dishari.in.web.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.List;

public record CreateBioPageRequest(

        @NotBlank(message = "Handle is required")
        @Size(min = 3, max = 32,
                message = "Handle must be between 3 and 32 characters")
        @Pattern(regexp = "^[a-zA-Z0-9_]+$",
                message = "Handle can only contain letters, " +
                        "numbers and underscores")
        String handle,

        @Size(max = 128,
                message = "Display name max 128 characters")
        String displayName,

        @Size(max = 1024,
                message = "Bio max 1024 characters")
        String bio,

        String avatarUrl,

        @Valid
        List<CreateBioLinkRequest> links  // optional — can add later
) {}