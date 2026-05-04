package com.dishari.in.web.dto.request;

import com.dishari.in.domain.enums.DeviceType;
import com.dishari.in.domain.enums.UrlStatus;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.Instant;

public record UrlFilterRequest(

        // ── Available to ALL users ───────────────────────────────────
        String q,               // search by title or slug (LIKE)
        UrlStatus status,       // ACTIVE, EXPIRED, DISABLED, FLAGGED

        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
        Instant from,           // createdAt range start

        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
        Instant to,             // createdAt range end

        // ── PREMIUM only filters ─────────────────────────────────────
        String countryCode,     // ISO 3166-1 alpha-2 e.g. "IN", "US"
        DeviceType deviceType,   // MOBILE, DESKTOP, TABLET, BOT
        String tag              // filter by tag name
) {
        public UrlFilterRequest {
                if (from != null && to != null && from.isAfter(to)) {
                        throw new IllegalArgumentException(
                                "'from' date must be before 'to' date.");
                }
                if (countryCode != null && countryCode.length() != 2) {
                        throw new IllegalArgumentException(
                                "Country code must be exactly 2 characters (ISO 3166-1 alpha-2).");
                }
        }
}