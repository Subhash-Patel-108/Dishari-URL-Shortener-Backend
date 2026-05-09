package com.dishari.in.infrastructure.redirect.resolver;

import com.dishari.in.domain.entity.DeviceRule;
import com.dishari.in.domain.entity.ShortUrl;
import com.dishari.in.domain.enums.DeviceType;
import com.dishari.in.domain.repository.DeviceRuleRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class DeviceRuleResolver implements RuleResolver {

    private final DeviceRuleRepository deviceRuleRepository;

    @Override
    public Optional<String> resolve(ShortUrl url, HttpServletRequest request) {

        if (!url.isHasDeviceRule()) return Optional.empty();

        // ── 1. Detect device from User-Agent ─────────────────────
        String userAgent  = request.getHeader("User-Agent");
        DeviceType device = detectDevice(userAgent);

        log.debug("Device detected: {} slug={}", device, url.getSlug());

        // ── 2. Load rules ─────────────────────────────────────────
        List<DeviceRule> rules = deviceRuleRepository
                .findByShortUrlId(url.getId());

        if (rules.isEmpty()) return Optional.empty();

        // ── 3. Match exact device type ────────────────────────────
        Optional<String> exactMatch = rules.stream()
                .filter(r -> !r.isDefault())
                .filter(r -> r.getDeviceType() == device)
                .findFirst()
                .map(DeviceRule::getDestinationUrl);

        if (exactMatch.isPresent()) {
            log.debug("Device exact match: device={} slug={} dest={}",
                    device, url.getSlug(), exactMatch.get());
            return exactMatch;
        }

        return Optional.empty() ;
    }

    public Optional<String> resolveDefault(ShortUrl url) {
        List<DeviceRule> rules = deviceRuleRepository
                .findByShortUrlId(url.getId());

        // ── 4. Fall back to default device rule ───────────────────
        Optional<String> defaultRule = rules.stream()
                .filter(DeviceRule::isDefault)
                .findFirst()
                .map(DeviceRule::getDestinationUrl);

        if (defaultRule.isPresent()) {
            log.debug("Device default rule used: slug={}", url.getSlug());
        }

        return defaultRule;
    }

    // ── User-Agent parsing ───────────────────────────────────────
    private DeviceType detectDevice(String userAgent) {
        if (userAgent == null || userAgent.isBlank()) {
            return DeviceType.UNKNOWN;
        }

        String ua = userAgent.toLowerCase();

        // Bot detection first — before mobile/desktop
        if (isBot(ua)) return DeviceType.BOT;

        // Tablet — must check before mobile
        // iPad identifies itself as mobile in some cases
        if (ua.contains("tablet")
                || ua.contains("ipad")
                || (ua.contains("android")
                && !ua.contains("mobile"))) {
            return DeviceType.TABLET;
        }

        // Mobile
        if (ua.contains("mobile")
                || ua.contains("iphone")
                || ua.contains("ipod")
                || ua.contains("android")
                || ua.contains("blackberry")
                || ua.contains("windows phone")) {
            return DeviceType.MOBILE;
        }

        return DeviceType.DESKTOP;
    }

    private boolean isBot(String ua) {
        return ua.contains("bot")
                || ua.contains("crawler")
                || ua.contains("spider")
                || ua.contains("slurp")
                || ua.contains("googlebot")
                || ua.contains("bingbot")
                || ua.contains("facebookexternalhit")
                || ua.contains("twitterbot")
                || ua.contains("linkedinbot")
                || ua.contains("whatsapp")
                || ua.contains("curl")
                || ua.contains("wget")
                || ua.contains("python-requests")
                || ua.contains("java/");
    }
}