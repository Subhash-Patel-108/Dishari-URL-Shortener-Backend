package com.dishari.in.application.service;

import com.dishari.in.web.dto.request.AddCustomDomainRequest;
import com.dishari.in.web.dto.response.CustomDomainResponse;
import jakarta.validation.Valid;

import java.util.List;
import java.util.UUID;

public interface CustomDomainService {
    List<CustomDomainResponse> getDomains(String workspaceId, UUID userId);

    CustomDomainResponse addDomain(String workspaceId, AddCustomDomainRequest request, UUID userId);
}
