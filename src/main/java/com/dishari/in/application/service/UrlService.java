package com.dishari.in.application.service;

import com.dishari.in.domain.entity.User;
import com.dishari.in.web.dto.request.*;
import com.dishari.in.web.dto.response.*;
import jakarta.validation.Valid;
import org.springframework.core.io.Resource;


import java.time.Instant;
import java.util.UUID;

public interface UrlService {

    NormalUrlResponse createNormalUrl(String email, CreateNormalUrlRequest request);

    CustomUrlResponse createCustomUrl(String email, CreateCustomUrlRequest request);

    PaginatedResponse<ShortUrlResponse> getUserUrls(UrlFilterRequest filter, User principal, int page, int size, String sortBy, String sortDir);

    UrlDetailResponse getUrlDetail(User principal, String id);

    ShortUrlUpdateResponse updateUrlData(User principal, ShortUrlUpdateRequest updateRequest, String id);

    MessageResponse deleteShortUrl(User principal, String id);

    UpdateUrlStatusResponse updateStatus(User principal, UpdateUrlStatusRequest updateRequest, String id);

    Resource getQrCodeResource(UUID uuid, int size, String fgColor, String bgColor, String logoUrl, String format, boolean regenerate, User principal);

    UrlAnalyticsResponse getAnalytics(User principal, String id, AnalyticsFilterRequest filter);

    MessageResponse createBulkUrl(User principal, CreateBulkUrlRequest request);

    BulkUrlResponse createShortUrlFormBulk(User user, BulkUrlRequest request);
}
