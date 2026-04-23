package com.dishari.in.web.controller;

import com.dishari.in.application.service.UrlService;
import com.dishari.in.domain.entity.User;
import com.dishari.in.web.dto.request.CreateCustomUrlRequest;
import com.dishari.in.web.dto.request.CreateNormalUrlRequest;
import com.dishari.in.web.dto.response.CustomUrlResponse;
import com.dishari.in.web.dto.response.NormalUrlResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/urls")
public class UrlController {

    private final UrlService urlService ;

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
    public ResponseEntity<?> createCustomUrl(
            @Valid @RequestBody CreateCustomUrlRequest request ,
            @AuthenticationPrincipal User user
    ){
        String email = user.getEmail() ;
        CustomUrlResponse response = urlService.createCustomUrl(email , request) ;
        return ResponseEntity.status(HttpStatus.CREATED).body(response) ;
    }
}
