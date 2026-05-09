package com.dishari.in.config;

import java.util.Set;

public class AppConstants {
    public static final String[] AUTH_PUBLIC_URLS = {
            "/api/v1/auth/register" ,
            "/api/v1/auth/login" ,
            "/api/v1/auth/refresh" ,
            "/api/v1/auth/verify-email/**" ,
            "/api/v1/auth/forgot-password" ,
            "/api/v1/auth/reset-password" ,
            "/api/v1/auth/resend-verification" ,
            "/{slug}" ,
            "/{slug}/unlock",
            "/{slug}/preview",
            "/v3/api-docs" ,
            "/v3/api-docs/**" ,
            "/swagger-ui.html" ,
            "/swagger-ui/**"
    } ;



    public static final Set<String> RESERVED_SLUGS = Set.of(
            "api",
            "admin",
            "bio",
            "dashboard",
            "login",
            "register",
            "docs",
            "health",
            "swagger",
            "actuator"
    );

    // All fields any user can sort by
    public static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
            "createdAt", "updatedAt", "expiresAt",
            "maxClicks", "title", "slug"
    );
}
