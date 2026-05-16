package com.dishari.in.web.projection;

import java.util.UUID;

public record BioLinkProjection(
        UUID id,
        String title,
        String url,
        String iconType,
        Integer position,
        Long clickCount,
        Boolean isActive,
        Boolean hasAdvancedConfig
) {}