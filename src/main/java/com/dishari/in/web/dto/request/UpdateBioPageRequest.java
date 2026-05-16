package com.dishari.in.web.dto.request;


import jakarta.validation.constraints.Size;

public record UpdateBioPageRequest(

        @Size(max = 128)
        String displayName,

        @Size(max = 1024)
        String bio,

        String avatarUrl,

        Boolean isActive
) {}
