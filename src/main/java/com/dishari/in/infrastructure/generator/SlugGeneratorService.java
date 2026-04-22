package com.dishari.in.infrastructure.generator;

import com.dishari.in.domain.repository.ShortUrlRepository;
import com.dishari.in.exception.SlugAlreadyTakenException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Set;

import static com.dishari.in.config.AppConstants.RESERVED_SLUGS;

@Service
@RequiredArgsConstructor
@Slf4j
public class SlugGeneratorService {

    private final SnowflakeIdGenerator snowflakeIdGenerator;
    private final Base62Encoder base62Encoder;
    private final ShortUrlRepository shortUrlRepository;

    @Value("${app.slug.default-length}")
    private int DEFAULT_SLUG_LENGTH;

    @Value("${app.slug.max-retry-attempts}")
    private int MAX_RETRY_ATTEMPTS;

    // ── Generate auto slug from Snowflake ID ─────────────────────
    public String generateSlug() {
        for (int attempt = 1; attempt <= MAX_RETRY_ATTEMPTS; attempt++) {
            long snowflakeId = snowflakeIdGenerator.nextId();
            String slug = base62Encoder.encode(snowflakeId);

            // Trim to default length — Snowflake Base62 is ~10-11 chars
            // We take the last 7 chars — most unique part
            String trimmedSlug = trimToLength(slug, DEFAULT_SLUG_LENGTH);

            if (isAvailable(trimmedSlug)) {
                log.debug("Generated slug={} from snowflakeId={} attempt={}",
                        trimmedSlug, snowflakeId, attempt);
                return trimmedSlug;
            }

            log.warn("Slug collision on attempt={} slug={}", attempt, trimmedSlug);
        }

        // Fallback — use full Snowflake Base62 (no trimming, guaranteed unique)
        long snowflakeId = snowflakeIdGenerator.nextId();
        String fullSlug = base62Encoder.encode(snowflakeId);
        log.warn("Falling back to full snowflake slug={}", fullSlug);
        return fullSlug;
    }

    // ── Validate a custom slug provided by the user ──────────────
    public void validateCustomSlug(String customSlug) {
        if (customSlug == null || customSlug.isBlank()) {
            throw new IllegalArgumentException("Custom slug cannot be blank.");
        }
        if (customSlug.length() < 3 || customSlug.length() > 32) {
            throw new IllegalArgumentException(
                    "Custom slug must be between 3 and 32 characters.");
        }
        if (!customSlug.matches("^[a-zA-Z0-9_-]+$")) {
            throw new IllegalArgumentException(
                    "Custom slug can only contain letters, numbers, hyphens and underscores.");
        }
        if (RESERVED_SLUGS.contains(customSlug.toLowerCase())) {
            throw new IllegalArgumentException(
                    "'" + customSlug + "' is a reserved slug.");
        }
        if (!isAvailable(customSlug)) {
            throw new SlugAlreadyTakenException(
                    "Slug '" + customSlug + "' is already taken.");
        }
    }

    // ── Resolve slug — custom or auto-generated ──────────────────
    public String resolveSlug(String customSlug) {
        if (customSlug != null && !customSlug.isBlank()) {
            validateCustomSlug(customSlug);
            return customSlug;
        }
        return generateSlug();
    }

    // ── Helpers ──────────────────────────────────────────────────
    private boolean isAvailable(String slug) {
        if (RESERVED_SLUGS.contains(slug.toLowerCase())) return false;
        return !shortUrlRepository.existsBySlug(slug);
    }

    private String trimToLength(String slug, int length) {
        if (slug.length() <= length) return slug;
        // Take last N chars — higher entropy end of Base62 Snowflake
        return slug.substring(slug.length() - length);
    }
}