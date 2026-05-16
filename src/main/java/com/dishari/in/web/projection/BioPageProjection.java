package com.dishari.in.web.projection;

import java.util.UUID;

public record BioPageProjection(
        UUID id,
        String handle,
        String displayName,
        String bio,
        String avatarUrl,
        Long viewCount,
        Boolean isActive
) {}