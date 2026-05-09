// GeoDistributionResponse.java
package com.dishari.in.web.dto.response.analytics;

import com.dishari.in.web.dto.response.CountryStats;

import java.util.List;

public record GeoDistributionResponse(
        long                totalClicks,
        List<CountryStats>  countries,
        String              topCountry,
        int                 totalCountries  // distinct countries seen
) {}