package com.dishari.in.web.controller;

import com.dishari.in.application.service.RedirectService;
import com.dishari.in.infrastructure.messaging.producer.ClickEventProducer;
import com.dishari.in.infrastructure.redirect.RedirectData;
import com.dishari.in.utils.IPUtils;
import com.dishari.in.web.dto.request.UnlockSlugRequest;
import com.dishari.in.web.dto.response.RedirectPreviewResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@Slf4j
@RestController
@RequiredArgsConstructor
public class RedirectController {

    private final RedirectService redirectService ;
    private final ClickEventProducer clickEventProducer ;


    //TODO: Changing the Response from Location Header to normal DTO response
    @GetMapping("/{slug}")
    public ResponseEntity<Void> redirect (
            @PathVariable("slug") String slug ,
            HttpServletRequest request
    ) {
        String referer = request.getHeader("Referer") ;

        //TODO: Implement Variant for A/B Testing

        RedirectData data = redirectService.redirect(slug , request , false) ;

        if (data.resolvedBy().equals("SIMPLE") || data.resolvedBy().equals("RULE_ENGINE")) {
            log.debug("Published a Click Event :: shortUrlId = {}" , data.shortUrlId());
            String ipAddress = IPUtils.extractIpAddress(request) ;
            String userAgent = IPUtils.extractUserAgent(request) ;
            clickEventProducer.publishClickEvent(data.shortUrlId() , ipAddress , userAgent , null , referer);
        }
        log.debug("Redirect Controller :: shortUrlId = {} | destination = {} | resolvedBy = {}" , data.shortUrlId() , data.destination() , data.resolvedBy());
        return ResponseEntity.status(data.httpStatus()).location(URI.create(data.destination())).build() ;
    }


    @PostMapping("/{slug}/unlock")
    public ResponseEntity<Void> unlock(
            @Valid @RequestBody UnlockSlugRequest slugRequest ,
            @PathVariable("slug") String slug ,
            HttpServletRequest request
    ) {

        String referer = request.getHeader("Referer") ;

        RedirectData redirectData = redirectService.unlock(slug , slugRequest , request ) ;

        if (redirectData.resolvedBy().equals("SIMPLE") || redirectData.resolvedBy().equals("RULE_ENGINE")) {
            log.debug("Published a Click Event :: Password Protected :: shortUrlId = {}" , redirectData.shortUrlId());
            String ipAddress = IPUtils.extractIpAddress(request) ;
            String userAgent = IPUtils.extractUserAgent(request) ;
            clickEventProducer.publishClickEvent(redirectData.shortUrlId() , ipAddress , userAgent , null , referer);
        }
        return ResponseEntity.status(redirectData.httpStatus()).location(URI.create(redirectData.destination())).build() ;
    }

    @GetMapping("/{slug}/preview")
    public ResponseEntity<RedirectPreviewResponse> preview(
            @PathVariable("slug") String slug ,
            HttpServletRequest request
    ) {

        RedirectPreviewResponse previewResponse = redirectService.redirectPreview(slug , request) ;

        return ResponseEntity.status(HttpStatus.OK).body(previewResponse) ;
    }
}
