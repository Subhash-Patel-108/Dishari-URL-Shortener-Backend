package com.dishari.in.web.dto.response;

public record ReferrerStats(
        String referrerDomain,
        long clicks,
        double percentage
) {}
