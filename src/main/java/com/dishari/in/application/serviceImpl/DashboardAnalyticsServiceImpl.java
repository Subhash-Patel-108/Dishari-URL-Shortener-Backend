package com.dishari.in.application.serviceImpl;

import com.dishari.in.application.service.DashboardAnalyticsService;
import com.dishari.in.domain.entity.User;
import com.dishari.in.domain.repository.DashboardAnalyticsRepository;
import com.dishari.in.web.dto.response.ClickTrend;
import com.dishari.in.web.dto.response.CountryStats;
import com.dishari.in.web.dto.response.DeviceStats;
import com.dishari.in.web.dto.response.ReferrerStats;
import com.dishari.in.web.dto.response.analytics.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class DashboardAnalyticsServiceImpl implements DashboardAnalyticsService {

    private final DashboardAnalyticsRepository repo;

    @Value("${app.qr.base-url}")
    private String baseUrl;

    // ── Overview ─────────────────────────────────────────────────

    @Transactional(readOnly = true)
    @Override
    public DashboardOverviewResponse getOverview(User principal, int topLinksLimit) {

        UUID userId = principal.getId() ;
        String strUserId = userId.toString();
        Instant now = Instant.now();

        // ── Period boundaries ────────────────────────────────────
        Instant startOfToday    = now.truncatedTo(ChronoUnit.DAYS);
        Instant startOfWeek     = now.minus(7,  ChronoUnit.DAYS);
        Instant startOfMonth    = now.minus(30, ChronoUnit.DAYS);
        Instant previousMonth   = now.minus(60, ChronoUnit.DAYS);

        // ── Totals ───────────────────────────────────────────────
        long totalUrls  = repo.countTotalUrlsByUser(userId);
        long activeUrls  = repo.countActiveUrlsByUser(userId);
        long totalClicks = repo.countTotalClicksByUser(userId, null, null);
        long totalUniqueClicks = repo.countUniqueClicksByUser(userId, null, null);

        // ── Period clicks ────────────────────────────────────────
        long clicksToday = repo.countTotalClicksByUser(userId, startOfToday, now);
        long clicksThisWeek = repo.countTotalClicksByUser(userId, startOfWeek, now);
        long clicksThisMonth = repo.countTotalClicksByUser(userId, startOfMonth, now);

        // ── Growth rates — current vs previous 30 days ───────────
        long clicksPreviousMonth = repo.countTotalClicksByUser(userId, previousMonth, startOfMonth);

        double clickGrowthRate = calculateGrowthRate(clicksThisMonth, clicksPreviousMonth);

        // ── Top links ────────────────────────────────────────────
        List<TopLinkResponse> topLinks = buildTopLinks(userId, startOfMonth, now, topLinksLimit);

        // ── 7-day trend ──────────────────────────────────────────
        List<ClickTrend> recentTrend = buildRecentTrend(userId, startOfWeek);

        log.debug(
                "Dashboard overview: userId={} totalClicks={} " +
                        "clicksThisMonth={} growth={}%",
                userId, totalClicks, clicksThisMonth, clickGrowthRate
        );

        return new DashboardOverviewResponse(
                totalUrls,
                totalClicks,
                totalUniqueClicks,
                activeUrls,
                clicksToday,
                clicksThisWeek,
                clicksThisMonth,
                clickGrowthRate,
                0.0,            // urlGrowthRate — add if needed
                topLinks,
                recentTrend,
                now
        );
    }

    // ── Top Links ─────────────────────────────────────────────────

    @Transactional(readOnly = true)
    @Override
    public List<TopLinkResponse> getTopLinks(User principal, Instant from, Instant to, int limit) {

        Instant resolvedFrom = from != null
                ? from
                : Instant.now().minus(30, ChronoUnit.DAYS);

        Instant resolvedTo   = to != null ? to : Instant.now();

        return buildTopLinks(principal.getId(), resolvedFrom, resolvedTo, limit);
    }

    // ── Geo Distribution ─────────────────────────────────────────

    @Transactional(readOnly = true)
    @Override
    public GeoDistributionResponse getGeoDistribution(User principal, Instant from, Instant to, int limit) {

        UUID userId = principal.getId();

        Instant f = from != null
                ? from
                : Instant.now().minus(30, ChronoUnit.DAYS);

        Instant t = to != null ? to : Instant.now();

        List<Object[]> raw = repo.findGeoDistributionByUser(userId.toString(), f, t, limit);

        long total = raw.stream()
                .mapToLong(r -> ((Number) r[2]).longValue())
                .sum();

        List<CountryStats> countries = raw.stream()
                .map(row -> {
                    long clicks = ((Number) row[2]).longValue();
                    return new CountryStats(
                            nullSafe(row[0]),
                            nullSafe(row[1]),
                            clicks,
                            percentage(clicks, total)
                    );
                })
                .toList();

        int distinctCountries = repo.countDistinctCountriesByUser(userId.toString(), f, t);

        String topCountry = countries.isEmpty()
                ? null
                : countries.get(0).countryCode();

        return new GeoDistributionResponse(total, countries, topCountry, distinctCountries);
    }

    // ── Device Distribution ──────────────────────────────────────

    @Transactional(readOnly = true)
    @Override
    public DeviceDistributionResponse getDeviceDistribution(User principal, Instant from, Instant to) {

        UUID userId = principal.getId();
        Instant f = from != null
                ? from
                : Instant.now().minus(30, ChronoUnit.DAYS);

        Instant t = to != null ? to : Instant.now();

        List<Object[]> raw = repo.findDeviceDistributionByUser(userId.toString(), f, t);

        long total = raw.stream()
                .mapToLong(r -> ((Number) r[1]).longValue())
                .sum();

        List<DeviceStats> devices = raw.stream()
                .map(row -> {
                    long clicks = ((Number) row[1]).longValue();
                    return new DeviceStats(
                            nullSafe(row[0]),
                            clicks,
                            percentage(clicks, total)
                    );
                })
                .toList();

        String dominantDevice = devices.isEmpty()
                ? null
                : devices.get(0).deviceType();

        return new DeviceDistributionResponse(total, devices, dominantDevice);
    }

    // ── Referrer Distribution ────────────────────────────────────

    @Transactional(readOnly = true)
    @Override
    public ReferrerDistributionResponse getReferrerDistribution(User principal, Instant from, Instant to, int limit) {

        UUID    userId = principal.getId();
        Instant f      = from != null
                ? from : Instant.now().minus(30, ChronoUnit.DAYS);
        Instant t      = to != null ? to : Instant.now();

        List<Object[]> raw = repo.findReferrerDistributionByUser(
                userId.toString(), f, t, limit);

        long directClicks = repo.countDirectClicksByUser(userId.toString(), f, t);

        long totalFromReferrers = raw.stream()
                .mapToLong(r -> ((Number) r[1]).longValue())
                .sum();

        long totalClicks = totalFromReferrers + directClicks;

        List<ReferrerStats> referrers = raw.stream()
                .map(row -> {
                    long clicks = ((Number) row[1]).longValue();
                    return new ReferrerStats(
                            nullSafe(row[0]),
                            clicks,
                            percentage(clicks, totalClicks)
                    );
                })
                .toList();

        return new ReferrerDistributionResponse(
                totalClicks,
                directClicks,
                percentage(directClicks, totalClicks),
                referrers
        );
    }

    // ── Private helpers ──────────────────────────────────────────

    private List<TopLinkResponse> buildTopLinks(
            UUID userId, Instant from, Instant to, int limit) {

        List<Object[]> raw = repo.findTopLinksByUser(
                userId.toString(), from, to, limit);

        return raw.stream()
                .map(row -> {
                    UUID   id           = UUID.fromString(
                            String.valueOf(row[0]));
                    String slug         = nullSafe(row[1]);
                    String originalUrl  = nullSafe(row[2]);
                    String title        = nullSafe(row[3]);
                    long   totalClicks  = ((Number) row[4]).longValue();
                    long   uniqueClicks = ((Number) row[5]).longValue();

                    // Top country and device per link
                    String topCountry = findTopCountryForUrl(id);
                    String topDevice  = findTopDeviceForUrl(id);

                    return new TopLinkResponse(
                            id,
                            slug,
                            baseUrl + "/" + slug,
                            originalUrl,
                            title,
                            totalClicks,
                            uniqueClicks,
                            0.0,        // growthRate — add if needed
                            topCountry,
                            topDevice
                    );
                })
                .toList();
    }

    private List<ClickTrend> buildRecentTrend(UUID userId, Instant from) {

        return repo.findRecentTrendByUser(userId.toString(), from)
                .stream()
                .map(row -> new ClickTrend(
                        String.valueOf(row[0]),
                        ((Number) row[1]).longValue(),
                        ((Number) row[2]).longValue()
                ))
                .toList();
    }

    private String findTopCountryForUrl(UUID urlId) {
        List<Object[]> result = repo.findTopCountryForUrl(urlId.toString());
        return result.isEmpty() ? null : nullSafe(result.get(0)[0]);
    }

    private String findTopDeviceForUrl(UUID urlId) {
        List<Object[]> result = repo.findTopDeviceForUrl(urlId.toString());
        return result.isEmpty() ? null : nullSafe(result.get(0)[0]);
    }

    private double calculateGrowthRate(long current, long previous) {
        if (previous == 0) return current > 0 ? 100.0 : 0.0;
        double rate = ((double)(current - previous) / previous) * 100;
        return Math.round(rate * 100.0) / 100.0;
    }

    private double percentage(long part, long total) {
        if (total == 0) return 0.0;
        return Math.round((double) part / total * 10000.0) / 100.0;
    }

    private String nullSafe(Object obj) {
        return obj != null ? String.valueOf(obj) : null;
    }
}