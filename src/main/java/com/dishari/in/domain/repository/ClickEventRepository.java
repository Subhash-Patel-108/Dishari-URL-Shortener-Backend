package com.dishari.in.domain.repository;

import com.dishari.in.domain.entity.ClickEvent;
import com.dishari.in.domain.entity.ShortUrl;
import com.dishari.in.domain.enums.DeviceType;
import com.dishari.in.web.dto.response.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface ClickEventRepository extends JpaRepository<ClickEvent, UUID> {

    // ── Summary queries ──────────────────────────────────────────

    @Query("SELECT COUNT(c) FROM ClickEvent c " +
            "WHERE c.shortUrl.id = :id " +
            "AND (:from IS NULL OR c.clickedAt >= :from) " +
            "AND (:to   IS NULL OR c.clickedAt <= :to)")
    long countTotalClicks(
            @Param("id")   UUID id,
            @Param("from") Instant from,
            @Param("to")   Instant to);

    @Query("SELECT COUNT(c) FROM ClickEvent c " +
            "WHERE c.shortUrl.id = :id " +
            "AND c.unique = true " +
            "AND (:from IS NULL OR c.clickedAt >= :from) " +
            "AND (:to   IS NULL OR c.clickedAt <= :to)")
    long countUniqueClicks(
            @Param("id")   UUID id,
            @Param("from") Instant from,
            @Param("to")   Instant to);

    @Query("SELECT MIN(c.clickedAt) FROM ClickEvent c " +
            "WHERE c.shortUrl.id = :id")
    Instant findFirstClickAt(@Param("id") UUID id);

    @Query("SELECT MAX(c.clickedAt) FROM ClickEvent c " +
            "WHERE c.shortUrl.id = :id")
    Instant findLastClickAt(@Param("id") UUID id);

    // ── Time series queries ──────────────────────────────────────

    // DAY grouping
    @Query(value =
            "SELECT DATE(clicked_at) as period, " +
                    "COUNT(*) as totalClicks, " +
                    "SUM(CASE WHEN is_unique = true THEN 1 ELSE 0 END) as uniqueClicks " +
                    "FROM click_events " +
                    "WHERE short_url_id = :id " +
                    "AND (:from IS NULL OR clicked_at >= :from) " +
                    "AND (:to   IS NULL OR clicked_at <= :to) " +
                    "GROUP BY DATE(clicked_at) " +
                    "ORDER BY period ASC",
            nativeQuery = true)
    List<Object[]> findDailyTrends(
            @Param("id")   UUID id,
            @Param("from") Instant from,
            @Param("to")   Instant to);

    // WEEK grouping
    @Query(value =
            "SELECT YEARWEEK(clicked_at, 3) as period, " +
                    "COUNT(*) as totalClicks, " +
                    "SUM(CASE WHEN is_unique = true THEN 1 ELSE 0 END) as uniqueClicks " +
                    "FROM click_events " +
                    "WHERE short_url_id = :id " +
                    "AND (:from IS NULL OR clicked_at >= :from) " +
                    "AND (:to   IS NULL OR clicked_at <= :to) " +
                    "GROUP BY YEARWEEK(clicked_at, 3) " +
                    "ORDER BY period ASC",
            nativeQuery = true)
    List<Object[]> findWeeklyTrends(
            @Param("id")   UUID id,
            @Param("from") Instant from,
            @Param("to")   Instant to);

    // MONTH grouping
    @Query(value =
            "SELECT DATE_FORMAT(clicked_at, '%Y-%m') as period, " +
                    "COUNT(*) as totalClicks, " +
                    "SUM(CASE WHEN is_unique = true THEN 1 ELSE 0 END) as uniqueClicks " +
                    "FROM click_events " +
                    "WHERE short_url_id = :id " +
                    "AND (:from IS NULL OR clicked_at >= :from) " +
                    "AND (:to   IS NULL OR clicked_at <= :to) " +
                    "GROUP BY DATE_FORMAT(clicked_at, '%Y-%m') " +
                    "ORDER BY period ASC",
            nativeQuery = true)
    List<Object[]> findMonthlyTrends(
            @Param("id")   UUID id,
            @Param("from") Instant from,
            @Param("to")   Instant to);

    // HOUR grouping
    @Query(value =
            "SELECT DATE_FORMAT(clicked_at, '%Y-%m-%d %H:00') as period, " +
                    "COUNT(*) as totalClicks, " +
                    "SUM(CASE WHEN is_unique = true THEN 1 ELSE 0 END) as uniqueClicks " +
                    "FROM click_events " +
                    "WHERE short_url_id = :id " +
                    "AND (:from IS NULL OR clicked_at >= :from) " +
                    "AND (:to   IS NULL OR clicked_at <= :to) " +
                    "GROUP BY DATE_FORMAT(clicked_at, '%Y-%m-%d %H:00') " +
                    "ORDER BY period ASC",
            nativeQuery = true)
    List<Object[]> findHourlyTrends(
            @Param("id")   UUID id,
            @Param("from") Instant from,
            @Param("to")   Instant to);

    // ── Breakdown queries ────────────────────────────────────────

    @Query(value =
            "SELECT country, country_name, COUNT(*) as clicks " +
                    "FROM click_events " +
                    "WHERE short_url_id = :id " +
                    "AND (:from IS NULL OR clicked_at >= :from) " +
                    "AND (:to   IS NULL OR clicked_at <= :to) " +
                    "AND country IS NOT NULL " +
                    "GROUP BY country, country_name " +
                    "ORDER BY clicks DESC " +
                    "LIMIT :limit",
            nativeQuery = true)
    List<Object[]> findTopCountries(
            @Param("id")    UUID id,
            @Param("from")  Instant from,
            @Param("to")    Instant to,
            @Param("limit") int limit);

    @Query(value =
            "SELECT device, COUNT(*) as clicks " +
                    "FROM click_events " +
                    "WHERE short_url_id = :id " +
                    "AND (:from IS NULL OR clicked_at >= :from) " +
                    "AND (:to   IS NULL OR clicked_at <= :to) " +
                    "GROUP BY device " +
                    "ORDER BY clicks DESC",
            nativeQuery = true)
    List<Object[]> findDeviceBreakdown(
            @Param("id")   UUID id,
            @Param("from") Instant from,
            @Param("to")   Instant to);

    @Query(value =
            "SELECT browser, COUNT(*) as clicks " +
                    "FROM click_events " +
                    "WHERE short_url_id = :id " +
                    "AND (:from IS NULL OR clicked_at >= :from) " +
                    "AND (:to   IS NULL OR clicked_at <= :to) " +
                    "AND browser IS NOT NULL " +
                    "GROUP BY browser " +
                    "ORDER BY clicks DESC " +
                    "LIMIT :limit",
            nativeQuery = true)
    List<Object[]> findTopBrowsers(
            @Param("id")    UUID id,
            @Param("from")  Instant from,
            @Param("to")    Instant to,
            @Param("limit") int limit);

    @Query(value =
            "SELECT referer_domain, COUNT(*) as clicks " +
                    "FROM click_events " +
                    "WHERE short_url_id = :id " +
                    "AND (:from IS NULL OR clicked_at >= :from) " +
                    "AND (:to   IS NULL OR clicked_at <= :to) " +
                    "AND referer_domain IS NOT NULL " +
                    "GROUP BY referer_domain " +
                    "ORDER BY clicks DESC " +
                    "LIMIT :limit",
            nativeQuery = true)
    List<Object[]> findTopReferrers(
            @Param("id")    UUID id,
            @Param("from")  Instant from,
            @Param("to")    Instant to,
            @Param("limit") int limit);

    // ── Filter-aware count — for filtered queries ────────────────
    @Query("SELECT COUNT(c) FROM ClickEvent c " +
            "WHERE c.shortUrl.id = :id " +
            "AND (:from    IS NULL OR c.clickedAt    >= :from) " +
            "AND (:to      IS NULL OR c.clickedAt    <= :to) " +
            "AND (:country IS NULL OR c.country      =  :country) " +
            "AND (:device  IS NULL OR c.device       =  :device) " +
            "AND (:browser IS NULL OR c.browser      =  :browser)")
    long countWithFilters(
            @Param("id")      UUID id,
            @Param("from")    Instant from,
            @Param("to")      Instant to,
            @Param("country") String country,
            @Param("device") DeviceType device,
            @Param("browser") String browser);


    Page<ClickEvent> findAllByShortUrl(ShortUrl shortUrl , Pageable pageable) ;
}