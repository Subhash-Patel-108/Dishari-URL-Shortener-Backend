package com.dishari.in.domain.repository;

import com.dishari.in.domain.entity.DeviceRule;
import com.dishari.in.domain.enums.DeviceType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DeviceRuleRepository extends JpaRepository<DeviceRule, UUID> {
    List<DeviceRule> findByShortUrlId(UUID shortUrlId) ;

    Optional<DeviceRule> findByShortUrlIdAndDeviceType(
            UUID shortUrlId, DeviceType deviceType);

    Optional<DeviceRule> findByShortUrlIdAndIsDefaultTrue(UUID shortUrlId);
}