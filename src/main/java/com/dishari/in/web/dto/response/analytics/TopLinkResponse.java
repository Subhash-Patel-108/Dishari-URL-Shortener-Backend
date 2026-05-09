// TopLinkResponse.java
package com.dishari.in.web.dto.response.analytics;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record TopLinkResponse(
        UUID   id,
        String slug,
        String shortUrl,
        String originalUrl,
        String title,
        long   totalClicks,
        long   uniqueClicks,
        double clickGrowthRate,     // % change vs previous period
        String topCountry,          // country driving most traffic
        String topDevice            // device driving most traffic
) {}