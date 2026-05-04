package com.dishari.in.web.dto.request;

import com.dishari.in.domain.enums.DeviceType;
import com.dishari.in.exception.InvalidEnumValueException;
import com.dishari.in.utils.EnumUtils;

import java.time.Instant;

public record AnalyticsFilterRequest(
        Instant from,
        Instant to,
        String groupBy,       // HOUR, DAY, WEEK, MONTH
        String country,       // ISO 3166-1 alpha-2
        DeviceType device,
        String browser,
        int limit             // max items in breakdown lists
) {
    // ── Compact constructor — validation ─────────────────────────
    public AnalyticsFilterRequest {
        if (from != null && to != null && from.isAfter(to)) {
            throw new IllegalArgumentException(
                    "'from' must be before 'to'.");
        }
        if (limit < 1 || limit > 100) {
            throw new IllegalArgumentException(
                    "limit must be between 1 and 100.");
        }
        if (groupBy != null) {
            groupBy = groupBy.toUpperCase();
            if (!groupBy.matches("HOUR|DAY|WEEK|MONTH")) {
                throw new InvalidEnumValueException(
                        "Invalid groupBy: '" + groupBy +
                                "'. Allowed: HOUR, DAY, WEEK, MONTH");
            }
        }
    }

    // ── Resolve default date range if not provided ───────────────
    public Instant resolvedFrom() {
        return from != null
                ? from
                : Instant.now().minus(30, java.time.temporal.ChronoUnit.DAYS);
    }

    public Instant resolvedTo() {
        return to != null ? to : Instant.now();
    }

    public String resolvedGroupBy() {
        return groupBy != null ? groupBy : "DAY";
    }
}