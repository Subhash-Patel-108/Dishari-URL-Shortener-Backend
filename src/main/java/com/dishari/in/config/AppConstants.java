package com.dishari.in.config;

public class AppConstants {
    public static final String[] AUTH_PUBLIC_URLS = {
            "/api/v1/auth/register" ,
            "/api/v1/auth/login" ,
            "/api/v1/auth/refresh" ,
            "/api/v1/auth/verify-email/**" ,
            "/api/v1/auth/forgot-password" ,
            "/api/v1/auth/reset-password" ,
            "/api/v1/auth/resend-verification" ,
            "/v3/api-docs" ,
            "/v3/api-docs/**" ,
            "/swagger-ui.html" ,
            "/swagger-ui/**"
    } ;
}
