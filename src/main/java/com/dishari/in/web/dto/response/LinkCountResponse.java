package com.dishari.in.web.dto.response;

public record LinkCountResponse(
        long activeLinksCount,
        int remainingSlots,
        int maxLinksAllowed,
        boolean canAddMore
) {}
