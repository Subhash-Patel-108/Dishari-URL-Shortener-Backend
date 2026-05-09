package com.dishari.in.domain.repository;

import com.dishari.in.domain.entity.GeoRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface GeoRuleRepository extends JpaRepository<GeoRule, UUID> {


    List<GeoRule> findByShortUrlIdOrderByPriorityAsc(UUID shortUrlId);

    Optional<GeoRule> findByShortUrlIdAndCountryCodeIgnoreCase(
            UUID shortUrlId, String countryCode);

    Optional<GeoRule> findByShortUrlIdAndIsDefaultTrue(UUID shortUrlId);

    List<GeoRule> findByShortUrlId(UUID shortUrlId);
}