package com.dishari.in.infrastructure.cache;

import com.dishari.in.exception.RedisOperationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class RedisService {

    private final RedisTemplate<String, String> redisTemplate;

    // ── Core Operations ─────────────────────────────────────────

    public void set(String key, String value, Duration ttl) {
        try {
            redisTemplate.opsForValue().set(key, value, ttl);
        } catch (Exception ex) {
            log.error("Redis SET failed for key={} : {}", key, ex.getMessage());
            throw new RedisOperationException("Cache write failed");
        }
    }

    public Optional<String> get(String key) {
        try {
            String value = redisTemplate.opsForValue().get(key);
            return Optional.ofNullable(value);
        } catch (Exception ex) {
            log.warn("Redis GET failed for key={} : {}", key, ex.getMessage());
            return Optional.empty(); // fail open — don't break the app
        }
    }

    public boolean delete(String key) {
        try {
            return Boolean.TRUE.equals(redisTemplate.delete(key));
        } catch (Exception ex) {
            log.error("Redis DELETE failed for key={} : {}", key, ex.getMessage());
            return false;
        }
    }

    public boolean exists(String key) {
        try {
            return Boolean.TRUE.equals(redisTemplate.hasKey(key));
        } catch (Exception ex) {
            log.warn("Redis EXISTS failed for key={} : {}", key, ex.getMessage());
            return false; // fail open
        }
    }

    // ── Atomic increment — used for click counters ──────────────
    public long increment(String key) {
        try {
            Long result = redisTemplate.opsForValue().increment(key);
            return result != null ? result : 0L;
        } catch (Exception ex) {
            log.error("Redis INCR failed for key={} : {}", key, ex.getMessage());
            return 0L;
        }
    }

    // ── Increment with TTL on first creation ────────────────────
    public long incrementWithTtl(String key, Duration ttl) {
        try {
            Long count = redisTemplate.opsForValue().increment(key);
            if (count != null && count == 1) {
                // First increment — set TTL for the window
                redisTemplate.expire(key, ttl);
            }
            return count != null ? count : 0L;
        } catch (Exception ex) {
            log.error("Redis INCR+TTL failed for key={} : {}", key, ex.getMessage());
            return 0L;
        }
    }

    public void setIfAbsent(String key, String value, Duration ttl) {
        try {
            redisTemplate.opsForValue().setIfAbsent(key, value, ttl);
        } catch (Exception ex) {
            log.error("Redis SETNX failed for key={} : {}", key, ex.getMessage());
        }
    }

    /**
     * Executes a Lua script for atomic operations.
     * * @param script   The Lua script string.
     * @param keys     List of keys involved in the script.
     * @param args     Arguments for the script (e.g., TTL).
     * @return List of strings returned by the script.
     */
    public List<String> executeScript(String script, List<String> keys, Object... args) {
        try {
            DefaultRedisScript<List> redisScript = new DefaultRedisScript<>();
            redisScript.setScriptText(script);
            redisScript.setResultType(List.class);

            // Spring's RedisTemplate handles the serialization and execution
            return redisTemplate.execute(redisScript, keys, args);
        } catch (Exception ex) {
            log.error("Redis Lua script execution failed: {}", ex.getMessage());
            return Collections.emptyList();
        }
    }
}