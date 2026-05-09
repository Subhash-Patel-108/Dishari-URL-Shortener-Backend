package com.dishari.in.web.controller;


import com.dishari.in.application.service.DashboardAnalyticsService;
import com.dishari.in.domain.entity.User;
import com.dishari.in.web.dto.response.analytics.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/v1/analytics")
@RequiredArgsConstructor
public class DashboardAnalyticsController {

    private final DashboardAnalyticsService dashboardAnalyticsService;

    // ── GET /api/v1/analytics/overview ───────────────────────────
    @GetMapping("/overview")
    public ResponseEntity<DashboardOverviewResponse> getOverview(
            @RequestParam(defaultValue = "5") int topLinksLimit,
            @AuthenticationPrincipal User principal) {

        return ResponseEntity.ok(dashboardAnalyticsService.getOverview(principal, topLinksLimit));
    }

    // ── GET /api/v1/analytics/top-links ──────────────────────────
    @GetMapping("/top-links")
    public ResponseEntity<List<TopLinkResponse>> getTopLinks(
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to,
            @RequestParam(defaultValue = "10") int limit,
            @AuthenticationPrincipal User principal) {

        return ResponseEntity.ok(
                dashboardAnalyticsService.getTopLinks(
                        principal,
                        parseInstant(from),
                        parseInstant(to),
                        limit
                ));
    }

    // ── GET /api/v1/analytics/geo ─────────────────────────────────
    @GetMapping("/geo")
    public ResponseEntity<GeoDistributionResponse> getGeoDistribution(
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to,
            @RequestParam(defaultValue = "20") int limit,
            @AuthenticationPrincipal User principal) {

        return ResponseEntity.ok(
                dashboardAnalyticsService.getGeoDistribution(
                        principal,
                        parseInstant(from),
                        parseInstant(to),
                        limit
                ));
    }

    // ── GET /api/v1/analytics/devices ────────────────────────────
    @GetMapping("/devices")
    public ResponseEntity<DeviceDistributionResponse> getDeviceDistribution(
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to,
            @AuthenticationPrincipal User principal) {

        return ResponseEntity.ok(
                dashboardAnalyticsService.getDeviceDistribution(
                        principal,
                        parseInstant(from),
                        parseInstant(to)
                ));
    }

    // ── GET /api/v1/analytics/referrers ──────────────────────────
    @GetMapping("/referrers")
    public ResponseEntity<ReferrerDistributionResponse> getReferrerDistribution(
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to,
            @RequestParam(defaultValue = "20") int limit,
            @AuthenticationPrincipal User principal) {

        return ResponseEntity.ok(
                dashboardAnalyticsService.getReferrerDistribution(
                        principal,
                        parseInstant(from),
                        parseInstant(to),
                        limit
                ));
    }

    private Instant parseInstant(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            return Instant.parse(value);
        } catch (Exception ex) {
            throw new IllegalArgumentException(
                    "Invalid date format: '" + value +
                            "'. Use ISO-8601: 2024-01-15T00:00:00Z");
        }
    }
}