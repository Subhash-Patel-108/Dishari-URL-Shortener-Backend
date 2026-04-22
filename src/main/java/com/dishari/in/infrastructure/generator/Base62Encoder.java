package com.dishari.in.infrastructure.generator;

import org.springframework.stereotype.Component;

@Component
public class Base62Encoder {

    private static final String ALPHABET =
            "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";

    private static final int BASE = 62;

    // ── Encode long → Base62 string ──────────────────────────────
    public String encode(long value) {
        if (value == 0) return "0";

        StringBuilder sb = new StringBuilder();
        long remaining = value;

        while (remaining > 0) {
            sb.append(ALPHABET.charAt((int)(remaining % BASE)));
            remaining /= BASE;
        }

        // Result is built in reverse — flip it
        return sb.reverse().toString();
    }

    // ── Decode Base62 string → long ──────────────────────────────
    public long decode(String encoded) {
        long result = 0;
        for (char c : encoded.toCharArray()) {
            result = result * BASE + ALPHABET.indexOf(c);
        }
        return result;
    }

    // ── Pad to fixed length with leading zeros ───────────────────
    public String encodeWithPadding(long value, int targetLength) {
        String encoded = encode(value);
        if (encoded.length() >= targetLength) return encoded;

        return "0".repeat(targetLength - encoded.length()) + encoded;
    }
}