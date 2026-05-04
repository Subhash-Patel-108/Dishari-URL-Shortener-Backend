package com.dishari.in.web.dto.response;

import java.util.List;
import java.util.UUID;

public record BulkReport(
        UUID jobId ,
        int totalProcessed ,
        List<String> successfulShortUrls ,
        List<BulkErrorDetail> failures
) {}
