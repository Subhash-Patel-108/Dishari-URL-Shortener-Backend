package com.dishari.in.infrastructure.cache;

import org.springframework.stereotype.Component;

@Component
public final class RedisKeys {
    private RedisKeys() {}

    // ── Email Verification ──────────────────────────────────────
    // Value: userId
    public static String emailVerification(String token) {
        return "email:verify:" + token;
    }

    // Rate limit for verification email resend — Value: count
    public static String emailVerifyRateLimit(String email) {
        return "email:verify:ratelimit:" + email;
    }

    // ── URL Redirect Cache ──────────────────────────────────────
    // Value: originalUrl
    public static String urlRedirect(String slug) {
        return "url:redirect:" + slug;
    }

    // Click counter — Value: long count (synced to DB periodically)
    public static String urlClickCount(String slug) {
        return "url:clicks:" + slug;
    }

    // ── Token Blacklist ─────────────────────────────────────────
    // Value: "revoked" — access token JTI blacklist
    public static String accessTokenBlacklist(String jti) {
        return "blacklist:access:" + jti;
    }
}
