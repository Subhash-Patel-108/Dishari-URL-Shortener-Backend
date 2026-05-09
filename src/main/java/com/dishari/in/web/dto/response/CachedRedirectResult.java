package com.dishari.in.web.dto.response;

public record CachedRedirectResult(
        String metadata,
        long clickCount
) {
}
