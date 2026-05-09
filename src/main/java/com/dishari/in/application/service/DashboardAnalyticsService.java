package com.dishari.in.application.service;

import com.dishari.in.domain.entity.User;
import com.dishari.in.web.dto.response.analytics.*;

import java.time.Instant;
import java.util.List;

public interface DashboardAnalyticsService {
    DashboardOverviewResponse getOverview(User principal, int topLinksLimit);

    List<TopLinkResponse> getTopLinks(User principal, Instant from, Instant to, int limit);

    GeoDistributionResponse getGeoDistribution(User principal, Instant from, Instant to, int limit);

    DeviceDistributionResponse getDeviceDistribution(User principal, Instant from, Instant to);

    ReferrerDistributionResponse getReferrerDistribution(User principal, Instant from, Instant to, int limit);
}
