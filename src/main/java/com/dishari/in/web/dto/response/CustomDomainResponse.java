package com.dishari.in.web.dto.response;

import com.dishari.in.domain.enums.CustomDomainStatus;
import com.dishari.in.domain.enums.VerificationType;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;

public record CustomDomainResponse(
        String id,
        String domain,
        String rootRedirect,
        String errorRedirect,
        boolean verified,
        String verificationToken,
        VerificationType verificationType,
        CustomDomainStatus status,
        boolean sslEnabled,
        Instant verifiedAt,
        Instant createdAt,
        Instant updatedAt
) {}