package com.dishari.in.infrastructure.redirect.resolver;

import com.dishari.in.domain.entity.LinkRotation;
import com.dishari.in.domain.entity.RotationDestination;
import com.dishari.in.domain.entity.ShortUrl;
import com.dishari.in.domain.repository.LinkRotationRepository;
import com.dishari.in.infrastructure.geoip.GeoIpService;
import com.dishari.in.utils.IPUtils;
import com.dishari.in.web.mapper.TimeZoneMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.DateTimeException;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

@Component
@RequiredArgsConstructor
@Slf4j
public class LinkRotationResolver implements RuleResolver {

    private final LinkRotationRepository linkRotationRepository;
    private final RedisTemplate<String, String> redisTemplate;
    private final GeoIpService geoIpService;

    private static final String ROTATION_KEY_PREFIX = "rotation:counter:";

    @Override
    public Optional<String> resolve(ShortUrl url, HttpServletRequest request) {

        if (!url.isHasLinkRotation()) return Optional.empty();

        LinkRotation rotation = linkRotationRepository
                .findActiveRotationWithDestinations(url.getId())
                .orElse(null);

        if (rotation == null) {
            log.warn("hasLinkRotation=true but no rotation config found for slug={}", url.getSlug());
            return Optional.empty();
        }

        // Filter active destinations and sort by position (important!)
        List<RotationDestination> activeDestinations = rotation.getRotationDestinations().stream()
                .filter(RotationDestination::isActive)
                .toList();

        if (activeDestinations.isEmpty()) {
            log.warn("No active destinations found for slug={}", url.getSlug());
            return Optional.empty();
        }

        String destination = switch (rotation.getRotationStrategy()) {
            case ROUND_ROBIN -> resolveRoundRobin(url, activeDestinations);
            case WEIGHTED    -> resolveWeighted(url, activeDestinations);
            case SCHEDULED   -> resolveScheduled(url, activeDestinations,request );
        };

        log.debug("Link Rotation resolved → Strategy={} | Slug={} | Destination={}",
                rotation.getRotationStrategy(), url.getSlug(), destination);

        return Optional.ofNullable(destination);
    }

    // ====================== SCHEDULED STRATEGY (Fixed) ======================
    private String resolveScheduled(ShortUrl url, List<RotationDestination> destinations , HttpServletRequest request) {

        // 1. Get Client IP (using your existing utility)
        String clientIp = IPUtils.extractIpAddress(request);

        GeoIpService.GeoLocation geoLocation = geoIpService.getLocation(clientIp);

        // Default to UTC if location or timezone is unavailable
        ZoneId userZone = ZoneId.of("UTC");

        if (geoLocation.timeZone() != null) {
            try {
                userZone = ZoneId.of(geoLocation.timeZone());
            } catch (DateTimeException e) {
                log.error("Invalid timezone format from GeoIP for IP {}: {}", clientIp, geoLocation.timeZone());
            }
        }

        // 4. Get "Now" in the USER's local time
        LocalTime now = LocalTime.now(userZone).withNano(0);
        // Find first matching time window (respecting position order)
        Optional<RotationDestination> matched = destinations.stream()
                .filter(dest -> dest.isActiveAt(now))
                .findFirst();

        if (matched.isPresent()) {
            log.debug("Scheduled rotation matched time window: {} -> {}", now, matched.get().getDestinationUrl());
            return matched.get().getDestinationUrl();
        }

        // Fallback: Use destination with no time restriction (always active)
        Optional<RotationDestination> alwaysOn = destinations.stream()
                .filter(d -> d.getActiveFrom() == null && d.getActiveTo() == null)
                .findFirst();

        if (alwaysOn.isPresent()) {
            return alwaysOn.get().getDestinationUrl();
        }

        // Ultimate fallback
        log.warn("No scheduled destination matched for time={}, using first destination", now);
        return destinations.get(0).getDestinationUrl();
    }

    // ====================== Other Strategies (unchanged) ======================
    private String resolveRoundRobin(ShortUrl url, List<RotationDestination> destinations) {

        List<RotationDestination> activeDestinations = destinations.stream()
                .sorted(Comparator.comparingInt(d -> d.getPosition() != null ? d.getPosition() : 999))
                .toList();
        String key = ROTATION_KEY_PREFIX + url.getId();
        Long counter = redisTemplate.opsForValue().increment(key);
        if (counter == null) counter = 0L;

        int index = (int) ((counter - 1) % activeDestinations.size());
        return activeDestinations.get(index).getDestinationUrl();
    }

    private String resolveWeighted(ShortUrl url, List<RotationDestination> destinations) {
        int totalWeight = destinations.stream()
                .mapToInt(d -> d.getWeight() != null ? d.getWeight() : 1)
                .sum();

        if (totalWeight <= 0) {
            return destinations.get(0).getDestinationUrl();
        }

        int rand = ThreadLocalRandom.current().nextInt(totalWeight);
        int cumulative = 0;

        for (RotationDestination dest : destinations) {
            cumulative += (dest.getWeight() != null ? dest.getWeight() : 1);
            if (rand < cumulative) {
                return dest.getDestinationUrl();
            }
        }
        return destinations.get(0).getDestinationUrl();
    }
}