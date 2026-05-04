package com.dishari.in.web.dto.response;

public record DeviceStats(
        String deviceType,      // MOBILE, DESKTOP, TABLET, UNKNOWN
        long clicks,
        double percentage
) {
}
