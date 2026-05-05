package com.dishari.in.domain.specification;

import com.dishari.in.domain.entity.*;
import com.dishari.in.domain.enums.DeviceType;
import com.dishari.in.domain.enums.UrlStatus;
import com.dishari.in.exception.PlanUpgradeRequiredException;
import com.dishari.in.web.dto.request.UrlFilterRequest;
import jakarta.persistence.criteria.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;

@Slf4j
public class ShortUrlSpecification {

    // ── Entry point — builds full spec based on user plan ────────
    public static Specification<ShortUrl> buildSpec(
            UrlFilterRequest filter,
            User principal) {

        Specification<ShortUrl> specification = Specification
                .where(belongsToUser(principal))
                .and(notDeleted()) ;

        if(filter.status() != null ) {
            specification = specification.and(hasStatus(filter.status())) ;
        }

        if(filter.q() != null ) {
            specification = specification.and(searchQuery(filter.q())) ;
        }

        if(filter.from() != null ) {
            specification = specification.and(createdAfter(filter.from())) ;
        }

        if(filter.from() != null ) {
            specification = specification.and(createdBefore(filter.to())) ;
        }

        if (hasPremiumAccess(principal)) {
            specification = addPremiumSpecification(specification , filter) ;
        }else {
            //Here we check that if user has no premium Previously and want to filter by premium features then we throw an exception to upgrade the plan
            if (filter.countryCode() != null || filter.deviceType() != null || filter.tag() != null) {
                throw new PlanUpgradeRequiredException("To access filtering by GeoRule , DeviceRule and Tag you must upgrade the plan to premium") ;
            }
        }
        return specification ;
    }

    // ── Core filters ─────────────────────────────────────────────

    public static Specification<ShortUrl> belongsToUser(User user) {
        return (root, query, cb) ->
                cb.equal(root.get("user").get("id"), user.getId());
    }

    public static Specification<ShortUrl> notDeleted() {
        return (root, query, cb) ->
                cb.isNull(root.get("deletedAt"));
    }

    public static Specification<ShortUrl> hasStatus(UrlStatus status) {
        if (status == null) return null;
        return (root, query, cb) ->
                cb.equal(root.get("status"), status);
    }

    public static Specification<ShortUrl> searchQuery(String q) {
        if (q == null || q.isBlank()) return null;
        return (root, query, cb) -> {
            String pattern = "%" + q.toLowerCase() + "%";
            return cb.or(
                    cb.like(cb.lower(root.get("slug")), pattern),
                    cb.like(cb.lower(root.get("title")), pattern),
                    cb.like(cb.lower(root.get("originalUrl")), pattern)
            );
        };
    }

    public static Specification<ShortUrl> hasTag(String tagName) {
        if (tagName == null || tagName.isBlank()) return null;
        return (root, query, cb) -> {
            Join<ShortUrl, Tag> tagJoin = root.join("tags", JoinType.INNER);
            return cb.equal(
                    cb.lower(tagJoin.get("name")),
                    tagName.toLowerCase()
            );
        };
    }

    public static Specification<ShortUrl> createdAfter(Instant from) {
        if (from == null) return null;
        return (root, query, cb) ->
                cb.greaterThanOrEqualTo(root.get("createdAt"), from);
    }

    public static Specification<ShortUrl> createdBefore(Instant to) {
        if (to == null) return null;
        return (root, query, cb) ->
                cb.lessThanOrEqualTo(root.get("createdAt"), to);
    }

    // ── Premium-only filters ─────────────────────────────────────

    // Filter URLs that have a geo rule for a specific country
    public static Specification<ShortUrl> hasGeoRule(String countryCode) {
        if (countryCode == null || countryCode.isBlank()) return null;
        return (root, query, cb) -> {
            // Subquery — ShortUrl must have a GeoRule with this country
            Subquery<Long> subquery = query.subquery(Long.class);
            Root<GeoRule> geoRoot = subquery.from(GeoRule.class);
            subquery.select(cb.literal(1L))
                    .where(
                            cb.equal(geoRoot.get("shortUrl").get("id"),
                                    root.get("id")),
                            cb.equal(
                                    cb.upper(geoRoot.get("country")),
                                    countryCode.toUpperCase()
                            )
                    );
            return cb.exists(subquery);
        };
    }

    // Filter URLs that have a device rule for a specific device type
    public static Specification<ShortUrl> hasDeviceRule(DeviceType deviceType) {
        if (deviceType == null) return null;
        return (root, query, cb) -> {
            Subquery<Long> subquery = query.subquery(Long.class);
            Root<DeviceRule> deviceRoot =
                    subquery.from(DeviceRule.class);
            subquery.select(cb.literal(1L))
                    .where(
                            cb.equal(deviceRoot.get("shortUrl").get("id"),
                                    root.get("id")),
                            cb.equal(deviceRoot.get("deviceType"), deviceType)
                    );
            return cb.exists(subquery);
        };
    }

    // ── Plan check helper ────────────────────────────────────────
    private static boolean hasPremiumAccess(User user) {
        return user.isHasPremium() || (user.getPlanExpiry() != null && user.getPlanExpiry().isAfter(Instant.now()));
    }

    private static Specification<ShortUrl> addPremiumSpecification(Specification<ShortUrl> specification , UrlFilterRequest filter) {
        if (filter.deviceType() != null)
            specification = specification.and(hasDeviceRule(filter.deviceType())) ;

        if(filter.tag() != null)
            specification = specification.and(hasTag(filter.tag())) ;

        if (filter.countryCode() != null)
            specification = specification.and(hasGeoRule(filter.countryCode())) ;

        return specification ;
    }
}