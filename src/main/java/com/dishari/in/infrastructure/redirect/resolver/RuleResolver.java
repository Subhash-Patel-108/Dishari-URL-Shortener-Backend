package com.dishari.in.infrastructure.redirect.resolver;

import com.dishari.in.domain.entity.ShortUrl;
import jakarta.servlet.http.HttpServletRequest;

import java.util.Optional;

public interface RuleResolver {
    Optional<String> resolve(ShortUrl url, HttpServletRequest request);
}
