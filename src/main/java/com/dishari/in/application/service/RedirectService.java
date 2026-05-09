package com.dishari.in.application.service;

import com.dishari.in.infrastructure.redirect.RedirectData;
import com.dishari.in.web.dto.request.UnlockSlugRequest;
import com.dishari.in.web.dto.response.RedirectPreviewResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

public interface RedirectService {
    RedirectData redirect(String slug , HttpServletRequest servletRequest , boolean isPasswordChecked );

    RedirectData unlock(String slug ,UnlockSlugRequest slugRequest, HttpServletRequest request);

    RedirectPreviewResponse redirectPreview(String slug, HttpServletRequest request);
}
