package com.dishari.in.web.dto.response;

import com.dishari.in.domain.entity.ShortUrl;

import java.time.Instant;
import java.util.List;

public record UrlAnalyticsResponse(
        String shortUrlId,
        String slug,
        String title,
        String originalUrl,

        // Overall Summary
        AnalyticsSummary summary,

        // Time series data (for charts)
        List<ClickTrend> trends,

        // Breakdowns
        List<CountryStats> topCountries,
        List<DeviceStats> topDevices,
        List<BrowserStats> topBrowsers,
        List<ReferrerStats> topReferrers,

        // Metadata
        Instant generatedAt,
        String periodDescription
) {

    public static UrlAnalyticsResponse from(
            ShortUrl shortUrl,
            AnalyticsSummary summary,
            List<ClickTrend> trends,
            List<CountryStats> topCountries,
            List<DeviceStats> topDevices,
            List<BrowserStats> topBrowsers,
            List<ReferrerStats> topReferrers,
            String periodDescription) {

        return new UrlAnalyticsResponse(
                shortUrl.getId().toString(),
                shortUrl.getSlug(),
                shortUrl.getTitle(),
                shortUrl.getOriginalUrl(),
                summary,
                trends,
                topCountries,
                topDevices,
                topBrowsers,
                topReferrers,
                Instant.now(),
                periodDescription
        );
    }
}
