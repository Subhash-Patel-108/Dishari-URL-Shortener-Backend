package com.dishari.in.utils;

import com.dishari.in.exception.UUIDParsingException;
import org.springframework.security.core.parameters.P;

import java.nio.ByteBuffer;
import java.util.UUID;

public final class UuidUtils {

    private UuidUtils() {}

    // ── Convert MySQL BINARY(16) bytes → UUID ────────────────────
    public static UUID fromBytes(Object value) {
        if (value == null) return null;

        // Already a String (some MySQL configs return string)
        if (value instanceof String str) {
            return UUID.fromString(str);
        }

        // Raw bytes from MySQL BINARY(16)
        if (value instanceof byte[] bytes) {
            if (bytes.length != 16) {
                throw new IllegalArgumentException(
                        "Expected 16 bytes for UUID, got: " + bytes.length);
            }
            ByteBuffer bb = ByteBuffer.wrap(bytes);
            long high = bb.getLong();
            long low  = bb.getLong();
            return new UUID(high, low);
        }

        // Fallback — try String conversion
        return UUID.fromString(String.valueOf(value));
    }

    // ── Safe version — returns null instead of throwing ──────────
    public static UUID fromBytesOrNull(Object value) {
        try {
            return fromBytes(value);
        } catch (Exception ex) {
            return null;
        }
    }

    //---Method to parse String into UUID
    public static UUID parse(String uuid) {
        try {
            return UUID.fromString(uuid) ;
        } catch (Exception ex) {
            throw new UUIDParsingException("Invalid UUID format.") ;
        }
    }
}