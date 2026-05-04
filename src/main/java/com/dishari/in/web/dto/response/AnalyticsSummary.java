package com.dishari.in.web.dto.response;

import java.time.Instant;

public record AnalyticsSummary(
        long totalClicks,
        long uniqueClicks,
        Instant firstClickAt,
        Instant lastClickAt,
        double averageClicksPerDay

) {}
