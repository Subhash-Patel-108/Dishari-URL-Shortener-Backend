package com.dishari.in.infrastructure.cache;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class ForgotPasswordCacheService {

    private final RedisService redisService;

    @Value("${spring.app.redis.ttl.forgot-password}")
    private long ttlSeconds;

    private static final int MAX_RESEND_PER_WINDOW = 3;
    private static final Duration RESEND_WINDOW = Duration.ofDays(1); // maximum 3 request per day for forgot password

    // ── Generate and store verification token ───────────────────

    public String generateAndStoreToken(String userId) {
        String token = UUID.randomUUID().toString().replace("-", "");
        String key = RedisKeys.forgotPassword(token);
        redisService.set(key, userId, Duration.ofSeconds(ttlSeconds));
        log.debug("Email verification token stored for userId={}", userId);
        return token;
    }

    // ── Validate and consume token (one-time use) ────────────────

    public Optional<String> validateAndConsume(String token) {
        String key = RedisKeys.forgotPassword(token);
        Optional<String> userId = redisService.get(key);

        if (userId.isPresent()) {
            redisService.delete(key); // one-time use — delete immediately
            log.debug("Email verification token consumed for userId={}", userId.get());
        } else {
            log.warn("Email verification token not found or expired: {}", token);
        }

        return userId;
    }

    // ── Rate limit resend requests ───────────────────────────────

    public boolean canResendVerification(String email) {
        String key = RedisKeys.forgotPasswordRateLimit(email);
        long count = redisService.incrementWithTtl(key, RESEND_WINDOW);
        return count <= MAX_RESEND_PER_WINDOW;
    }

    public boolean isTokenValid(String token) {
        return redisService.exists(RedisKeys.emailVerification(token));
    }
}
