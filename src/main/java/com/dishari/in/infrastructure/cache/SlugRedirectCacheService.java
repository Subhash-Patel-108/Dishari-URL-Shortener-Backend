package com.dishari.in.infrastructure.cache;

import com.dishari.in.web.dto.response.CachedRedirectResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class SlugRedirectCacheService {

    private final RedisService redisService;
    private final long ttlInSeconds = 60 * 15; // 15 minutes

    private static final String INCREMENT_AND_EXPIRE_SCRIPT =
            "local meta = redis.call('get', KEYS[1]) " +
                    "if meta then " +
                    "  local count = redis.call('incr', KEYS[2]) " +
                    "  redis.call('expire', KEYS[1], ARGV[1]) " +
                    "  redis.call('expire', KEYS[2], ARGV[1]) " +
                    "  return {meta, tostring(count)} " + // Explicitly return an array of 2 strings
                    "else " +
                    "  return {} " + // Return empty array instead of nil to avoid null pointer in Java
                    "end";

    public void put(String slug, String id, String originalUrl, boolean flagged,
                    Long maxClicks, Instant expiresAt, long clickCount, boolean hasPassword) {

        String key = RedisKeys.urlRedirect(slug);
        String clickCountKey = RedisKeys.urlClickCount(slug);

        // SDE-3: Use StringBuilder for performance over String.format
        StringBuilder value = new StringBuilder()
                .append(id).append("|")
                .append(originalUrl).append("|")
                .append(flagged).append("|")
                .append(maxClicks != null ? maxClicks : 0L).append("|")
                .append(expiresAt != null ? expiresAt.toString() : "null").append("|")
                .append(hasPassword);

        // Atomic multi-set could also be done via pipelining, but simple set is fine here
        redisService.set(key, value.toString(), Duration.ofSeconds(ttlInSeconds));
        redisService.set(clickCountKey, String.valueOf(clickCount), Duration.ofSeconds(ttlInSeconds));
    }

    /**
     * Executes Lua script to Get, Increment, and Refresh TTL in one atomic step.
     */
    public Optional<CachedRedirectResult> getAndIncrement(String slug) {
        String metaKey = RedisKeys.urlRedirect(slug);
        String countKey = RedisKeys.urlClickCount(slug);

        // Assuming your redisService has an executeScript method
        // If not, you can use StringRedisTemplate.execute(...)
        List<String> result = redisService.executeScript(
                INCREMENT_AND_EXPIRE_SCRIPT,
                List.of(metaKey, countKey),
                String.valueOf(ttlInSeconds)
        );

        if (result == null || result.isEmpty()) {
            return Optional.empty();
        }

        // result[0] is metadata, result[1] is the new click count
        return Optional.of(new CachedRedirectResult(
                result.get(0),
                Long.parseLong(result.get(1))
        ));
    }
}
