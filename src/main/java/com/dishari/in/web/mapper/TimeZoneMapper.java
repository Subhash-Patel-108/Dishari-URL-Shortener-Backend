package com.dishari.in.web.mapper;

import java.time.ZoneId;

public class TimeZoneMapper {
    /**
     * Maps a 2-letter ISO country code to a primary ZoneId.
     * Defaults to UTC if the country is unknown.
     */
    public static ZoneId getZoneForCountry(String countryCode) {
        if (countryCode == null || countryCode.isBlank()) {
            return ZoneId.of("UTC");
        }

        // Use ICU4J to get the "Golden Zone" (primary zone) for a country
        String[] zones = com.ibm.icu.util.TimeZone.getAvailableIDs(countryCode);

        if (zones != null && zones.length > 0) {
            // Usually, the first ID is the most representative/populous zone
            return ZoneId.of(zones[0]);
        }

        return ZoneId.of("UTC");
    }
}
