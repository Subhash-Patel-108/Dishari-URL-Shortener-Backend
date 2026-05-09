package com.dishari.in.infrastructure.redirect;

public record RedirectData(
        String shortUrlId,
        String destination,
        int    httpStatus,
        String resolvedBy    // CACHE, GEO_RULE, DEVICE_RULE, LINK_ROTATION, SIMPLE, PASSWORD_PROTECTED
) {
    // Convenience constructors
    public static RedirectData simple(String id, String url) {
        return new RedirectData(id, url, 302, "SIMPLE");
    }

    public static RedirectData unsafe(String id , String slug , String baseUrl) {
        String destination = baseUrl + "/unsafe-slug?slug" + slug ;
        return new RedirectData(id, destination, 451, "FLAGGED");
    }

    public static RedirectData expired(String id , String slug , String baseUrl) {
        String destination = baseUrl + "/slug-expired?slug=" + slug ;
        return new RedirectData(id, destination, 410, "EXPIRED");
    }

    public static RedirectData notFound(String slug , String baseUrl) {
        String destination = baseUrl + "/slug-not-found?slug=" + slug ;
        return new RedirectData(null, destination, 404, "NOT_FOUND");
    }

    public static RedirectData passwordProtected(String id, String slug, String baseUrl ) {
        String destination = baseUrl + "/password-protected?slug=" + slug ;
        return new RedirectData(id, destination, 403, "PASSWORD_PROTECTED");
    }
}