package com.dishari.in.application.service;

import com.dishari.in.web.dto.request.CreateCustomUrlRequest;
import com.dishari.in.web.dto.request.CreateNormalUrlRequest;
import com.dishari.in.web.dto.response.NormalUrlResponse;
import jakarta.validation.Valid;

public interface UrlService {

    NormalUrlResponse createNormalUrl(String email, CreateNormalUrlRequest request);

    NormalUrlResponse createCustomUrl(String email, CreateCustomUrlRequest request);
}
