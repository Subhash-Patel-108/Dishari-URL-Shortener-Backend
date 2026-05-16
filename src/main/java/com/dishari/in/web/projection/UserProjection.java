package com.dishari.in.web.projection;

public record UserProjection(
        String id,
        String username,
        String email,
        String displayName
) {}