package com.dishari.in.web.controller;

import com.dishari.in.annotation.RequiresPlan;
import com.dishari.in.application.service.UrlService;
import com.dishari.in.application.serviceImpl.QRCodeService;
import com.dishari.in.domain.entity.User;
import com.dishari.in.domain.enums.DeviceType;
import com.dishari.in.domain.enums.Plan;
import com.dishari.in.domain.enums.UrlStatus;
import com.dishari.in.utils.EnumUtils;
import com.dishari.in.web.dto.request.*;
import com.dishari.in.web.dto.response.*;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.UUID;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/urls")
public class UrlController {

    private final UrlService urlService ;
    private final QRCodeService qrCodeService ;

    //We make two endpoints for create url one for normal user and second one for the user with Premium plan (Custom slug)

    @PostMapping()
    public ResponseEntity<NormalUrlResponse> createNormalUrl(
            @Valid @RequestBody CreateNormalUrlRequest request ,
            @AuthenticationPrincipal User user
    ) {
        String email = user.getEmail() ;
        NormalUrlResponse response = urlService.createNormalUrl(email , request) ;
        return ResponseEntity.status(HttpStatus.CREATED).body(response) ;
    }

    @PostMapping("/custom")
    @RequiresPlan(
            value = {Plan.ENTERPRISE , Plan.PREMIUM , Plan.PRO} ,
            feature = "Custom slug creation"
    )
    public ResponseEntity<?> createCustomUrl(
            @Valid @RequestBody CreateCustomUrlRequest request ,
            @AuthenticationPrincipal User user
    ){
        String email = user.getEmail() ;
        CustomUrlResponse response = urlService.createCustomUrl(email , request) ;
        return ResponseEntity.status(HttpStatus.CREATED).body(response) ;
    }

    //Method to get all the Short Url by using filter, search and sort with pagination
    @GetMapping()
    public ResponseEntity<PaginatedResponse<ShortUrlResponse>> getShortUrls(
            // ── Pagination ───────────────────────────────────────
            @RequestParam(defaultValue = "0" , required = false)   int page,
            @RequestParam(defaultValue = "20" , required = false)  int size,
            @RequestParam(defaultValue = "createdAt" , required = false) String sortBy,
            @RequestParam(defaultValue = "desc" , required = false) String sortDir,

            // ── Filters available to ALL users ───────────────────
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to,

            // ── Filters for PREMIUM users only ───────────────────
            // These are accepted from everyone but only applied
            // for hasPremium=true users in the Specification layer
            @RequestParam(required = false) String countryCode,
            @RequestParam(required = false) String deviceType,
            @RequestParam(required = false) String tag,

            @AuthenticationPrincipal User principal
    ) {
        // ── Parse status enum safely → 400 on invalid value ──────
        UrlStatus parsedStatus = status != null
                ? EnumUtils.fromStringOrThrow(
                UrlStatus.class, status, "status")
                : null ;

        // ── Parse device type enum safely → 400 on invalid value ─
        DeviceType parsedDeviceType = deviceType != null
                ? EnumUtils.fromStringOrThrow(
                DeviceType.class, deviceType, "deviceType")
                : null ;

        // ── Parse dates safely → 400 on invalid format ───────────
        Instant parsedFrom = parseInstant(from, "from");
        Instant parsedTo   = parseInstant(to, "to");

        // ── Build filter — compact constructor validates range ────
        UrlFilterRequest filter = new UrlFilterRequest(
                q,
                parsedStatus,
                parsedFrom,
                parsedTo,
                countryCode,
                parsedDeviceType ,
                tag
        );
        return ResponseEntity.ok(
                urlService.getUserUrls(
                        filter, principal, page, size, sortBy, sortDir));
    }


    //API endpoint for metadata for particular short url
    @GetMapping("/{id}")
    public ResponseEntity<UrlDetailResponse> getUrlDetail(
            @AuthenticationPrincipal User principal ,
            @PathVariable("id") String id) {
        UrlDetailResponse response = urlService.getUrlDetail(principal , id) ;

        return ResponseEntity.status(HttpStatus.OK).body(response) ;
    }

    //API endpoint to update metadata of shortUrl
    @PutMapping("/{id}")
    public ResponseEntity<ShortUrlUpdateResponse> updateUrlData(
            @AuthenticationPrincipal User principal ,
            @Valid @RequestBody ShortUrlUpdateRequest updateRequest ,
            @PathVariable("id") String id){

        ShortUrlUpdateResponse response = urlService.updateUrlData(principal , updateRequest , id) ;
        return ResponseEntity.status(HttpStatus.OK).body(response) ;
    }

    //API endpoint to delete the shortUrl
    @DeleteMapping("/{id}")
    public ResponseEntity<MessageResponse> deleteShortUrl(
            @AuthenticationPrincipal User principal ,
            @PathVariable("id") String id
    ){
        MessageResponse response = urlService.deleteShortUrl(principal , id) ;
        return ResponseEntity.status(HttpStatus.OK).body(response) ;
    }


    //API endpoint to change the url status
    @PatchMapping("/{id}/status")
    public ResponseEntity<UpdateUrlStatusResponse> updateUrlStatus(
            @AuthenticationPrincipal User principal ,
            @Valid @RequestBody UpdateUrlStatusRequest updateRequest ,
            @PathVariable("id") String id
    ){
        UpdateUrlStatusResponse response = urlService.updateStatus(principal , updateRequest , id) ;
        return ResponseEntity.status(HttpStatus.OK).body(response) ;
    }

    @GetMapping("/{id}/qr")
    @RequiresPlan(
            value = {Plan.PRO, Plan.ENTERPRISE, Plan.PREMIUM},
            feature = "QR code download",
            checkExpiry = true
    )
    public ResponseEntity<Resource> downloadQrCode(
            @PathVariable String id,
            @RequestParam(defaultValue = "300") int size,
            @RequestParam(defaultValue = "#000000") String fgColor,
            @RequestParam(defaultValue = "#FFFFFF") String bgColor,
            @RequestParam(required = false) String logoUrl,
            @RequestParam(defaultValue = "PNG") String format,
            @RequestParam(defaultValue = "false") boolean download,
            @RequestParam(defaultValue = "false") boolean regenerate,
            @AuthenticationPrincipal User principal) {

        UUID uuid = UUID.fromString(id);

        String safeFgColor = URLDecoder.decode(fgColor , StandardCharsets.UTF_8) ;
        String safeBgColor = URLDecoder.decode(bgColor , StandardCharsets.UTF_8) ;
        Resource resource = urlService.getQrCodeResource(
                uuid, size, safeFgColor, safeBgColor, logoUrl, format, regenerate, principal);

        if (resource == null) {
            return ResponseEntity.notFound().build();
        }

        String contentType = "SVG".equalsIgnoreCase(format) ? "image/svg+xml" : "image/png";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(contentType));

        if (download) {
            headers.setContentDispositionFormData("attachment",
                    "qr-" + id + "." + format.toLowerCase());
        }

        headers.setCacheControl("max-age=300");

        return ResponseEntity.ok()
                .headers(headers)
                .body(resource);
    }

    //Endpoint api endpoint for ShortUrl Analytics
    @GetMapping("/{id}/analytics")
    public ResponseEntity<UrlAnalyticsResponse> shortUrlAnalytics(
            @AuthenticationPrincipal User principal,
            @PathVariable String id,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to,
            @RequestParam(defaultValue = "DAY") String groupBy,
            @RequestParam(required = false) String country,
            @RequestParam(required = false) String device,
            @RequestParam(required = false) String browser,
            @RequestParam(defaultValue = "10") int limit) {

        // ── Parse dates safely ───────────────────────────────────────
        Instant parsedFrom = from != null ? parseInstant(from, "from") : null;
        Instant parsedTo   = to   != null ? parseInstant(to,   "to")   : null;

        // ── Parse device enum safely ─────────────────────────────────
        DeviceType parsedDevice = device != null
                ? EnumUtils.fromStringOrThrow(
                DeviceType.class, device, "device")
                : null;

        // ── Build filter — compact constructor validates ─────────────
        AnalyticsFilterRequest filter = new AnalyticsFilterRequest(
                parsedFrom,
                parsedTo,
                groupBy,
                country,
                parsedDevice,
                browser,
                limit
        );

        return ResponseEntity.ok(urlService.getAnalytics(principal, id, filter));
    }


    //TODO: /{id}/analytics/export


    //Endpoint to create bulk Url async (max 100 per request)
    @PostMapping("/bulk")
    @RequiresPlan(
            value = {Plan.PRO, Plan.ENTERPRISE, Plan.PREMIUM},
            feature = "Bulk Url Creation",
            checkExpiry = true
    )
    public ResponseEntity<MessageResponse> createBulkUrl(
            @AuthenticationPrincipal User principal ,
            @RequestBody CreateBulkUrlRequest request
    ) {
        MessageResponse response = urlService.createBulkUrl(principal , request) ;
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response) ;
    }

    // ── Safe Instant parser ───────────────────────────────────────
    private Instant parseInstant(String value, String fieldName) {
        if (value == null || value.isBlank()) return null;
        try {
            return Instant.parse(value);
        } catch (DateTimeParseException ex) {
            throw new DateTimeParseException(
                    "Invalid date format for '" + fieldName +
                            "'. Use ISO-8601 e.g. 2024-01-15T00:00:00Z",
                    value, ex.getErrorIndex()
            );
        }
    }
}
