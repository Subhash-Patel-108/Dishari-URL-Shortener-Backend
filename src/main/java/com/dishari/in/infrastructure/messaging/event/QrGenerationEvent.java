package com.dishari.in.infrastructure.messaging.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class QrGenerationEvent {

    // ── Identifiers ──────────────────────────────────────────────
    private UUID   shortUrlId;
    private String slug;
    private String shortUrlString;  // full URL e.g. https://snap.url/abc123

    // ── QR options — set to defaults if not customized ───────────
    private int    size;
    private String fgColor;
    private String bgColor;
    private String logoUrl;         // null if no logo
    private String format;          // PNG or SVG

    // ── Metadata ─────────────────────────────────────────────────
    private UUID    userId;         // for logging and audit
    private Instant triggeredAt;    // when the event was published
    private int     attempt;        // retry tracking — starts at 1
}