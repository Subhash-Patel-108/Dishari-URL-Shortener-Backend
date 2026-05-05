package com.dishari.in.utils;

import com.dishari.in.web.dto.response.BulkErrorDetail;
import com.dishari.in.web.dto.response.BulkSuccessDetail;
import com.dishari.in.web.dto.response.BulkUrlResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

@Component
@Slf4j
public class CSVUtils {
    public byte[] generateBulkCsv(List<BulkUrlResponse> successUrls, List<BulkErrorDetail> failedUrls) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream();
             CSVPrinter printer = new CSVPrinter(new PrintWriter(out),
                     CSVFormat.DEFAULT.withHeader(
                             "Id",
                             "Short Url / Reason",
                             "Slug",
                             "Original URL",
                             "Title",
                             "Utm Source",
                             "Utm Medium",
                             "Utm Campaign",
                             "Utm Content",
                             "Utm Term",
                             "CreatedAt",
                             "ExpiresAt",
                             "Max Clicks",
                             "Is Password Protected",
                             "Status",
                             "Is Already Created"))) {

            // 1. Write Successful Deployments
            for (BulkUrlResponse url : successUrls) {
                printer.printRecord(
                        url.id(),
                        url.shortUrl(), // Mapped to Short Url / Reason
                        url.slug(),
                        url.originalUrl(),
                        url.title() != null ? url.title() : "N/A",
                        url.utm() != null ? url.utm().utmSource() : "N/A",
                        url.utm() != null ? url.utm().utmMedium() : "N/A",
                        url.utm() != null ? url.utm().utmCampaign() : "N/A",
                        url.utm() != null ? url.utm().utmContent() : "N/A",
                        url.utm() != null ? url.utm().utmTerm() : "N/A",
                        url.createdAt(), // Instant.toString() is already ISO-8601
                        url.expiresAt() != null ? url.expiresAt() : "NEVER",
                        url.maxClicks() != null ? url.maxClicks() : "UNLIMITED",
                        url.isPasswordProtected() ? "YES" : "NO",
                        "✅ SUCCESS",
                        url.isAlreadyCreated() ? "YES" : "NO"
                );
            }

            // 2. Write Failed Records
            for (BulkErrorDetail failure : failedUrls) {
                // For failures, we leave specific metadata as N/A or empty
                printer.printRecord(
                        "N/A",            // Id
                        failure.reason(), // Short Url / Reason
                        "N/A",            // Slug
                        failure.originalUrl(),
                        "N/A",            // Title
                        "N/A", "N/A", "N/A", "N/A", "N/A", // UTMs
                        "N/A",            // CreatedAt
                        "N/A",            // ExpiresAt
                        "N/A",            // Max Clicks
                        "N/A",            // Password Protected
                        "❌ FAILED",
                        "N/A"             // Is Already Created
                );
            }

            printer.flush();
            return out.toByteArray();
        } catch (IOException e) {
            log.error("Failed to generate Dishari Bulk CSV attachment", e);
            return new byte[0];
        }
    }
}
