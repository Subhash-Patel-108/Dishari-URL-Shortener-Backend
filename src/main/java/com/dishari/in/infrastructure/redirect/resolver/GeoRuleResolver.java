package com.dishari.in.infrastructure.redirect.resolver;

import com.dishari.in.domain.entity.GeoRule;
import com.dishari.in.domain.repository.GeoRuleRepository;
import com.dishari.in.infrastructure.geoip.GeoIpService;
import com.dishari.in.domain.entity.ShortUrl;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class GeoRuleResolver implements RuleResolver {

    private final GeoRuleRepository geoRuleRepository;
    private final GeoIpService      geoIpService;

    @Override
    public Optional<String> resolve(ShortUrl url, HttpServletRequest request) {

        if (!url.isHasGeoRule()) return Optional.empty();

        // ── 1. Detect country from IP ────────────────────────────
        String ip          = extractIp(request);
        String countryCode = geoIpService.getCountryCode(ip);

        if (countryCode == null) {
            log.debug("GeoIP returned null for ip={} slug={}", ip, url.getSlug());
            return Optional.empty() ;
        }

        log.debug("GeoIP resolved: ip={} country={} slug={}",
                ip, countryCode, url.getSlug());

        // ── 2. Load all geo rules for this URL ───────────────────
        List<GeoRule> rules = geoRuleRepository
                .findByShortUrlIdOrderByPriorityAsc(url.getId());

        if (rules.isEmpty()) return Optional.empty();

        // ── 3. Match exact country first ─────────────────────────
        Optional<String> exactMatch = rules.stream()
                .filter(r -> r.getCountryCode()
                        .equalsIgnoreCase(countryCode))
                .findFirst()
                .map(GeoRule::getDestinationUrl);

        if (exactMatch.isPresent()) {
            log.debug("Geo exact match: country={} slug={} dest={}",
                    countryCode, url.getSlug(), exactMatch.get());
            return exactMatch;
        }

        // ── 4. Fall back to default geo rule ─────────────────────
        return Optional.empty() ;
    }

    public  Optional<String> resolveDefault(ShortUrl url) {
        return geoRuleRepository
                .findByShortUrlIdOrderByPriorityAsc(url.getId())
                .stream()
                .filter(GeoRule::isDefault)
                .findFirst()
                .map(GeoRule::getDestinationUrl);
    }

    private Optional<String> resolveDefault(ShortUrl url, List<GeoRule> rules) {
        return rules.stream()
                .filter(GeoRule::isDefault)
                .findFirst()
                .map(GeoRule::getDestinationUrl);
    }

    private String extractIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}