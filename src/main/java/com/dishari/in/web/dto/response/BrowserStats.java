package com.dishari.in.web.dto.response;

public record BrowserStats(
        String browserName,
        long clicks,
        double percentage
) {
}
