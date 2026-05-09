package com.dishari.in.infrastructure.redirect;

import com.dishari.in.domain.entity.ShortUrl;
import com.dishari.in.infrastructure.redirect.resolver.DeviceRuleResolver;
import com.dishari.in.infrastructure.redirect.resolver.GeoRuleResolver;
import com.dishari.in.infrastructure.redirect.resolver.LinkRotationResolver;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class RuleEngine {

    private final LinkRotationResolver linkRotationResolver;
    private final GeoRuleResolver      geoRuleResolver;
    private final DeviceRuleResolver   deviceRuleResolver;

    // ── Resolution priority ──────────────────────────────────────
    // 1. LinkRotation  — overrides everything if active
    // 2. GeoRule       — country-based redirect
    // 3. DeviceRule    — device-based redirect
    // 4. Fallback      — url.getOriginalUrl()

    public String resolve(ShortUrl url, HttpServletRequest request) {

        log.debug(
                "RuleEngine.resolve: slug={} hasRotation={} hasGeo={} hasDevice={}",
                url.getSlug(),
                url.isHasLinkRotation(),
                url.isHasGeoRule(),
                url.isHasDeviceRule()
        );
        log.info(
                "RuleEngine.resolve: slug={} hasRotation={} hasGeo={} hasDevice={}",
                url.getSlug(),
                url.isHasLinkRotation(),
                url.isHasGeoRule(),
                url.isHasDeviceRule()
        );

        // ── 1. Geo Rule ──────────────────────────────────────────
        if (url.isHasGeoRule()) {
            Optional<String> geo = geoRuleResolver.resolve(url, request);
            if (geo.isPresent()) {
                log.debug("Resolved by GeoRule: slug={} dest={}", url.getSlug(), geo.get());
                log.info("Resolved by GeoRule: slug={} dest={}", url.getSlug(), geo.get());
                return geo.get();
            }
        }

        // ── 2. Device Rule ───────────────────────────────────────
        if (url.isHasDeviceRule()) {
            Optional<String> device = deviceRuleResolver.resolve(url, request);
            if (device.isPresent()) {
                log.debug("Resolved by DeviceRule: slug={} dest={}", url.getSlug(), device.get());
                log.info("Resolved by DeviceRule: slug={} dest={}", url.getSlug(), device.get());
                return device.get();
            }
        }

        // ── 3. Link Rotation ─────────────────────────────────────
        if (url.isHasLinkRotation()) {
            Optional<String> rotation = linkRotationResolver.resolve(url, request);
            if (rotation.isPresent()) {
                log.info("Link Rotation Destination URL : {} for slug : {}" , rotation.get(), url.getSlug());
                log.debug("Resolved by LinkRotation: slug={} dest={}", url.getSlug(), rotation.get());
                return rotation.get();
            }
        }

        //Now we apply the default geo rule
        if (url.isHasGeoRule()) {
            Optional<String> geo = geoRuleResolver.resolveDefault(url) ;

            if (geo.isPresent()) {
                log.debug("Resolved by GeoRule using Default : slug={} dest={}", url.getSlug(), geo.get());
                log.info("Resolved by GeoRule using Default : slug={} dest={}", url.getSlug(), geo.get());
                return geo.get();
            }
        }

        if (url.isHasDeviceRule()) {
            Optional<String> device = deviceRuleResolver.resolveDefault(url) ;

            if (device.isPresent()) {
                log.debug("Resolved by DeviceRule using Default : slug={} dest={}", url.getSlug(), device.get());
                log.info("Resolved by DeviceRule using Default : slug={} dest={}", url.getSlug(), device.get());
                return device.get();
            }
        }

        // ── 4. Fallback ──────────────────────────────────────────
        log.debug("Resolved by fallback: slug={} dest={}", url.getSlug(), url.getOriginalUrl());
        log.info("Resolved by fallback: slug={} dest={}", url.getSlug(), url.getOriginalUrl());
        return url.getOriginalUrl();
    }
}