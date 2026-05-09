// ReferrerDistributionResponse.java
package com.dishari.in.web.dto.response.analytics;

import com.dishari.in.web.dto.response.ReferrerStats;

import java.util.List;

public record ReferrerDistributionResponse(
        long                 totalClicks,
        long                 directClicks,       // no referrer
        double               directPercentage,
        List<ReferrerStats>  topReferrers
) {}