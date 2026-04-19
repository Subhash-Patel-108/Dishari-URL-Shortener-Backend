package com.dishari.in.utils;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Optional;

@Component

public class CookiesUtils {
    private final Long COOKIES_MAX_AGE_IN_SECONDS;
    private final String REFRESH_TOKEN_COOKIE_NAME;
    private final String ISSUER;
    private final Boolean COOKIE_SECURE;
    private final Boolean COOKIE_HTTP_ONLY;
    private final String COOKIE_SAME_SITE;
    private final String COOKIE_DOMAIN;
    private final String COOKIE_PATH;

    @Autowired
    private JwtUtils jwtUtils ;

    public CookiesUtils(
            @Value("${cookie.refrsh-token-cookie-name}") String REFRESH_TOKEN_COOKIE_NAME ,
            @Value("${cookie.issuer}") String ISSUER,
            @Value("${cookie.cookie-secure}") Boolean COOKIE_SECURE,
            @Value("${cookie.cookie-http-only}") Boolean COOKIE_HTTP_ONLY,
            @Value("${cookie.cookie-same-site}") String COOKIE_SAME_SITE,
            @Value("${cookie.cookie-domain}") String COOKIE_DOMAIN,
            @Value("${cookie.cookie-max-age-in-seconds}") Long cookieExpirationTimeInSeconds ,
            @Value("${cookie.cookie-path}") String COOKIE_PATH
    ) {
        this.REFRESH_TOKEN_COOKIE_NAME = REFRESH_TOKEN_COOKIE_NAME ;
        this.ISSUER = ISSUER;
        this.COOKIE_SECURE = COOKIE_SECURE;
        this.COOKIE_HTTP_ONLY = COOKIE_HTTP_ONLY;
        this.COOKIE_SAME_SITE = COOKIE_SAME_SITE;
        this.COOKIE_DOMAIN = COOKIE_DOMAIN;
        this.COOKIES_MAX_AGE_IN_SECONDS = cookieExpirationTimeInSeconds ;
        this.COOKIE_PATH = COOKIE_PATH;
    }


    //Cookie ---> Attach refresh token to the response
    public void attachRefreshTokenCookie(HttpServletResponse response, String refreshToken ) {
        ResponseCookie.ResponseCookieBuilder responseCookieBuilder = ResponseCookie.from(REFRESH_TOKEN_COOKIE_NAME, refreshToken)
                .maxAge(COOKIES_MAX_AGE_IN_SECONDS)
                .httpOnly(COOKIE_HTTP_ONLY)
                .path(COOKIE_PATH)
                .sameSite(COOKIE_SAME_SITE)
                .secure(COOKIE_SECURE) ;

        if (COOKIE_DOMAIN != null && !COOKIE_DOMAIN.trim().isEmpty() ) {
            responseCookieBuilder.domain(COOKIE_DOMAIN) ;
        }

        ResponseCookie responseCookie = responseCookieBuilder.build() ;

        response.addHeader(HttpHeaders.SET_COOKIE , responseCookie.toString());

        return ;
    }

    //Cookie ---> Clear refresh Cookie (Simply add the empty cookie on the same cookie name with max age as 0 second's)
    public void clearRefreshTokenCookieFromHeader(HttpServletResponse response) {
        ResponseCookie.ResponseCookieBuilder responseCookieBuilder = ResponseCookie.from(REFRESH_TOKEN_COOKIE_NAME, "")
                .maxAge(0)
                .path(COOKIE_PATH) // Ensure this is exactly "/"
                .secure(COOKIE_SECURE)
                .httpOnly(COOKIE_HTTP_ONLY)
                .sameSite(COOKIE_SAME_SITE) // Ensure this is "Lax"
                .domain(COOKIE_DOMAIN); // Ensure this matches exactly


        if (COOKIE_DOMAIN != null && !COOKIE_DOMAIN.trim().isEmpty()){
            responseCookieBuilder.domain(COOKIE_DOMAIN) ;
        }

        ResponseCookie cookieResponse = responseCookieBuilder.build() ;
        response.addHeader(HttpHeaders.SET_COOKIE , cookieResponse.toString());

        return ;
    }

    public void addNoStoreHeader(HttpServletResponse response) {
        // 1. Prevent Caching (Critical for Security)
        // 'no-store' tells the browser/proxies NOT to save the response on disk
        response.setHeader("Cache-Control", "no-cache, no-store, max-age=0, must-revalidate");
        response.setHeader("Pragma", "no-cache"); // For HTTP 1.0 backward compatibility
        response.setHeader("Expires", "0"); // Proxies should treat as immediately expired
    }

    //Method to read refresh token from Cookies or Request Body
    public Optional<String> extractRefreshTokenFromCookiesOrRequestBody(HttpServletRequest servletRequest) {
        //1. First we find refresh token from cookie
        if (servletRequest.getCookies() != null) {
            Optional<String> refreshCookie = Arrays.stream(servletRequest.getCookies())
                    .filter(cookie -> REFRESH_TOKEN_COOKIE_NAME.equals(cookie.getName()))
                    .map(Cookie::getValue)
                    .filter(value -> !value.trim().isEmpty())
                    .findFirst() ;

            if (refreshCookie.isPresent()) {
                return refreshCookie ;
            }
        }

        //2. If we have custom header
        String customHeader = servletRequest.getHeader("X-Refresh-Token");
        if (customHeader != null && !customHeader.trim().isEmpty()) {
            return Optional.of(customHeader) ;
        }

        //3. Passes in the form of Bearer <token>
        String bearerToken = servletRequest.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer")) {
            String candidate = bearerToken.substring(7) ;

            if (!candidate.trim().isEmpty() && jwtUtils.isRefreshToken(candidate)) {
                return Optional.of(candidate);
            }
        }

        //return empty if no refresh token found
        return Optional.empty();
    }
}
