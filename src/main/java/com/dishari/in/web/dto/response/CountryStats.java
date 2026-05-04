package com.dishari.in.web.dto.response;

public record CountryStats(
        String countryCode,
        String countryName,
        long clicks,
        double percentage
) {
}
