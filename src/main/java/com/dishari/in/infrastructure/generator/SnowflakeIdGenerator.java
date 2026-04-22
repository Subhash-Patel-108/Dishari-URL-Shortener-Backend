package com.dishari.in.infrastructure.generator;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class SnowflakeIdGenerator {

    // ── Bit allocation ───────────────────────────────────────────
    private static final long UNUSED_BITS       = 1L;  // sign bit — always 0
    private static final long TIMESTAMP_BITS    = 41L;
    private static final long NODE_ID_BITS      = 10L;
    private static final long SEQUENCE_BITS     = 12L;

    // ── Max values ───────────────────────────────────────────────
    private static final long MAX_NODE_ID       = ~(-1L << NODE_ID_BITS);   // 1023
    private static final long MAX_SEQUENCE      = ~(-1L << SEQUENCE_BITS);  // 4095

    // ── Bit shifts ───────────────────────────────────────────────
    private static final long NODE_ID_SHIFT     = SEQUENCE_BITS;            // 12
    private static final long TIMESTAMP_SHIFT   = NODE_ID_BITS + SEQUENCE_BITS; // 22

    private final long nodeId;
    private final long customEpoch;

    private long lastTimestamp = -1L;
    private long sequence      = 0L;

    public SnowflakeIdGenerator(
            @Value("${app.snowflake.node-id:1}") long nodeId,
            @Value("${app.snowflake.epoch:1700000000000}") long customEpoch) {

        if (nodeId < 0 || nodeId > MAX_NODE_ID) {
            throw new IllegalArgumentException(
                    "Node ID must be between 0 and " + MAX_NODE_ID);
        }

        this.nodeId      = nodeId;
        this.customEpoch = customEpoch;

        log.info("SnowflakeIdGenerator initialized — nodeId={} epoch={}",
                nodeId, customEpoch);
    }

    // ── Core generation — thread safe ───────────────────────────
    public synchronized long nextId() {
        long currentTimestamp = currentMs();

        // Clock moved backwards — reject to prevent duplicate IDs
        if (currentTimestamp < lastTimestamp) {
            long drift = lastTimestamp - currentTimestamp;
            log.error("Clock moved backwards by {}ms — rejecting ID generation", drift);
            throw new IllegalStateException(
                    "Clock moved backwards by " + drift + "ms. Refusing to generate ID.");
        }

        if (currentTimestamp == lastTimestamp) {
            // Same millisecond — increment sequence
            sequence = (sequence + 1) & MAX_SEQUENCE;

            if (sequence == 0) {
                // Sequence exhausted — wait for next ms
                currentTimestamp = waitForNextMs(lastTimestamp);
            }
        } else {
            // New millisecond — reset sequence
            sequence = 0L;
        }

        lastTimestamp = currentTimestamp;

        return ((currentTimestamp - customEpoch) << TIMESTAMP_SHIFT)
                | (nodeId << NODE_ID_SHIFT)
                | sequence;
    }

    // ── Parse a Snowflake ID back to its components ──────────────
    public SnowflakeComponents parse(long snowflakeId) {
        long timestamp = (snowflakeId >> TIMESTAMP_SHIFT) + customEpoch;
        long node      = (snowflakeId >> NODE_ID_SHIFT) & MAX_NODE_ID;
        long seq       = snowflakeId & MAX_SEQUENCE;
        return new SnowflakeComponents(snowflakeId, timestamp, node, seq);
    }

    // ── Helpers ──────────────────────────────────────────────────
    private long currentMs() {
        return System.currentTimeMillis();
    }

    private long waitForNextMs(long lastTs) {
        long ts = currentMs();
        while (ts <= lastTs) {
            ts = currentMs();
        }
        return ts;
    }

    // ── Value record for parsed components ──────────────────────
    public record SnowflakeComponents(
            long id,
            long timestampMs,
            long nodeId,
            long sequence
    ) {}
}