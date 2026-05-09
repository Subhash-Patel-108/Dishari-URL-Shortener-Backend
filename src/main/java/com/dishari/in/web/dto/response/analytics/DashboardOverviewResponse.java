// DashboardOverviewResponse.java
package com.dishari.in.web.dto.response.analytics;

import com.dishari.in.web.dto.response.ClickTrend;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record DashboardOverviewResponse(

        // ── Platform totals ──────────────────────────────────────────
        long totalUrls,
        long totalClicks,
        long totalUniqueClicks,
        long activeUrls,

        // ── Period comparison ────────────────────────────────────────
        long clicksToday,
        long clicksThisWeek,
        long clicksThisMonth,

        // ── Growth rates ─────────────────────────────────────────────
        double clickGrowthRate,     // % change vs previous period
        double urlGrowthRate,       // % change vs previous period

        // ── Top links snapshot ───────────────────────────────────────
        List<TopLinkResponse> topLinks,

        // ── Recent activity ──────────────────────────────────────────
        List<ClickTrend> recentTrend,   // last 7 days

        Instant generatedAt
) {}