// DeviceDistributionResponse.java
package com.dishari.in.web.dto.response.analytics;

import com.dishari.in.web.dto.response.DeviceStats;

import java.util.List;

public record DeviceDistributionResponse(
        long               totalClicks,
        List<DeviceStats>  devices,
        String             dominantDevice  // device with most clicks
) {}