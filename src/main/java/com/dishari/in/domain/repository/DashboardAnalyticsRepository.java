package com.dishari.in.domain.repository;

import com.dishari.in.domain.entity.ClickEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface DashboardAnalyticsRepository extends JpaRepository<ClickEvent, UUID> {


    @Query("SELECT COUNT(c) FROM ClickEvent c " +
            "WHERE c.shortUrl.user.id = :userId " +
            "AND (:from IS NULL OR c.clickedAt >= :from) " +
            "AND (:to   IS NULL OR c.clickedAt <= :to)")
    long countTotalClicksByUser(
            @Param("userId") UUID userId,
            @Param("from")   Instant from,
            @Param("to")     Instant to);

    @Query("SELECT COUNT(c) FROM ClickEvent c " +
            "WHERE c.shortUrl.user.id = :userId " +
            "AND c.unique = true " +
            "AND (:from IS NULL OR c.clickedAt >= :from) " +
            "AND (:to   IS NULL OR c.clickedAt <= :to)")
    long countUniqueClicksByUser(
            @Param("userId") UUID userId,
            @Param("from")   Instant from,
            @Param("to")     Instant to);

    @Query("SELECT COUNT(su) FROM ShortUrl su " +
            "WHERE su.user.id = :userId " +
            "AND su.deletedAt IS NULL")
    long countTotalUrlsByUser(@Param("userId") UUID userId);

    @Query("SELECT COUNT(su) FROM ShortUrl su " +
            "WHERE su.user.id = :userId " +
            "AND su.status = 'ACTIVE' " +
            "AND su.deletedAt IS NULL")
    long countActiveUrlsByUser(@Param("userId") UUID userId);

    // ── Top performing links ─────────────────────────────────────
    // Native query — must use:
    // 1. HEX() on returned id columns
    // 2. UNHEX(REPLACE()) on UUID parameters

    @Query(value =
            "SELECT " +
                    "LOWER(CONCAT(" +
                    "  SUBSTR(HEX(su.id),  1, 8), '-'," +
                    "  SUBSTR(HEX(su.id),  9, 4), '-'," +
                    "  SUBSTR(HEX(su.id), 13, 4), '-'," +
                    "  SUBSTR(HEX(su.id), 17, 4), '-'," +
                    "  SUBSTR(HEX(su.id), 21)    " +
                    ")) as id, " +
                    "su.slug, " +
                    "su.original_url, " +
                    "su.title, " +
                    "COUNT(c.id)                                              as total_clicks, " +
                    "SUM(CASE WHEN c.is_unique = true THEN 1 ELSE 0 END)     as unique_clicks " +
                    "FROM short_urls su " +
                    "LEFT JOIN click_events c ON c.short_url_id = su.id " +
                    "  AND (:from IS NULL OR c.clicked_at >= :from) " +
                    "  AND (:to   IS NULL OR c.clicked_at <= :to) " +
                    "WHERE su.user_id = UNHEX(REPLACE(:userId, '-', '')) " +
                    "AND su.deleted_at IS NULL " +
                    "GROUP BY su.id, su.slug, su.original_url, su.title " +
                    "ORDER BY total_clicks DESC " +
                    "LIMIT :limit",
            nativeQuery = true)
    List<Object[]> findTopLinksByUser(
            @Param("userId") String userId,   // ✅ String not UUID
            @Param("from")   Instant from,
            @Param("to")     Instant to,
            @Param("limit")  int limit);

    // ── Top country per link ─────────────────────────────────────

    @Query(value =
            "SELECT country, COUNT(*) as clicks " +
                    "FROM click_events " +
                    "WHERE short_url_id = UNHEX(REPLACE(:urlId, '-', '')) " +
                    "AND country IS NOT NULL " +
                    "GROUP BY country " +
                    "ORDER BY clicks DESC " +
                    "LIMIT 1",
            nativeQuery = true)
    List<Object[]> findTopCountryForUrl(@Param("urlId") String urlId);

    // ── Top device per link ──────────────────────────────────────

    @Query(value =
            "SELECT device, COUNT(*) as clicks " +
                    "FROM click_events " +
                    "WHERE short_url_id = UNHEX(REPLACE(:urlId, '-', '')) " +
                    "GROUP BY device " +
                    "ORDER BY clicks DESC " +
                    "LIMIT 1",
            nativeQuery = true)
    List<Object[]> findTopDeviceForUrl(
            @Param("urlId") String urlId);

    // ── Geo distribution ─────────────────────────────────────────

    @Query(value =
            "SELECT c.country, c.country_name, COUNT(*) as clicks " +
                    "FROM click_events c " +
                    "JOIN short_urls su ON c.short_url_id = su.id " +
                    "WHERE su.user_id = UNHEX(REPLACE(:userId, '-', '')) " +
                    "AND c.country IS NOT NULL " +
                    "AND (:from IS NULL OR c.clicked_at >= :from) " +
                    "AND (:to   IS NULL OR c.clicked_at <= :to) " +
                    "GROUP BY c.country, c.country_name " +
                    "ORDER BY clicks DESC " +
                    "LIMIT :limit",
            nativeQuery = true)
    List<Object[]> findGeoDistributionByUser(
            @Param("userId") String userId,
            @Param("from")   Instant from,
            @Param("to")     Instant to,
            @Param("limit")  int limit);

    @Query(value =
            "SELECT COUNT(DISTINCT c.country) " +
                    "FROM click_events c " +
                    "JOIN short_urls su ON c.short_url_id = su.id " +
                    "WHERE su.user_id = UNHEX(REPLACE(:userId, '-', '')) " +
                    "AND c.country IS NOT NULL " +
                    "AND (:from IS NULL OR c.clicked_at >= :from) " +
                    "AND (:to   IS NULL OR c.clicked_at <= :to)",
            nativeQuery = true)
    int countDistinctCountriesByUser(
            @Param("userId") String userId,
            @Param("from")   Instant from,
            @Param("to")     Instant to);

    // ── Device distribution ──────────────────────────────────────

    @Query(value =
            "SELECT c.device, COUNT(*) as clicks " +
                    "FROM click_events c " +
                    "JOIN short_urls su ON c.short_url_id = su.id " +
                    "WHERE su.user_id = UNHEX(REPLACE(:userId, '-', '')) " +
                    "AND (:from IS NULL OR c.clicked_at >= :from) " +
                    "AND (:to   IS NULL OR c.clicked_at <= :to) " +
                    "GROUP BY c.device " +
                    "ORDER BY clicks DESC",
            nativeQuery = true)
    List<Object[]> findDeviceDistributionByUser(
            @Param("userId") String userId,
            @Param("from")   Instant from,
            @Param("to")     Instant to);

    // ── Referrer distribution ────────────────────────────────────

    @Query(value =
            "SELECT c.referer_domain, COUNT(*) as clicks " +
                    "FROM click_events c " +
                    "JOIN short_urls su ON c.short_url_id = su.id " +
                    "WHERE su.user_id = UNHEX(REPLACE(:userId, '-', '')) " +
                    "AND c.referer_domain IS NOT NULL " +
                    "AND (:from IS NULL OR c.clicked_at >= :from) " +
                    "AND (:to   IS NULL OR c.clicked_at <= :to) " +
                    "GROUP BY c.referer_domain " +
                    "ORDER BY clicks DESC " +
                    "LIMIT :limit",
            nativeQuery = true)
    List<Object[]> findReferrerDistributionByUser(
            @Param("userId") String userId,
            @Param("from")   Instant from,
            @Param("to")     Instant to,
            @Param("limit")  int limit);

    @Query(value =
            "SELECT COUNT(*) FROM click_events c " +
                    "JOIN short_urls su ON c.short_url_id = su.id " +
                    "WHERE su.user_id = UNHEX(REPLACE(:userId, '-', '')) " +
                    "AND c.referer_domain IS NULL " +
                    "AND (:from IS NULL OR c.clicked_at >= :from) " +
                    "AND (:to   IS NULL OR c.clicked_at <= :to)",
            nativeQuery = true)
    long countDirectClicksByUser(
            @Param("userId") String userId,
            @Param("from")   Instant from,
            @Param("to")     Instant to);

    // ── 7-day trend ──────────────────────────────────────────────

    @Query(value =
            "SELECT DATE(c.clicked_at) as period, " +
                    "COUNT(*)                                                         as total_clicks, " +
                    "SUM(CASE WHEN c.is_unique = true THEN 1 ELSE 0 END)             as unique_clicks " +
                    "FROM click_events c " +
                    "JOIN short_urls su ON c.short_url_id = su.id " +
                    "WHERE su.user_id = UNHEX(REPLACE(:userId, '-', '')) " +
                    "AND c.clicked_at >= :from " +
                    "GROUP BY DATE(c.clicked_at) " +
                    "ORDER BY period ASC",
            nativeQuery = true)
    List<Object[]> findRecentTrendByUser(
            @Param("userId") String userId,
            @Param("from")   Instant from);
}