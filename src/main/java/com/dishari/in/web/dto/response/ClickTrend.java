package com.dishari.in.web.dto.response;

public record ClickTrend(
        String period,          // e.g., "2025-04-01", "2025-W14", "2025-04"
        long totalClicks,
        long uniqueClicks
) {}
