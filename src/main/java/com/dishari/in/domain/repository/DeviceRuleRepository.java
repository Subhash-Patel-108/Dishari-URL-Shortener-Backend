package com.dishari.in.domain.repository;

import com.dishari.in.domain.entity.DeviceRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface DeviceRuleRepository extends JpaRepository<DeviceRule, UUID> {
    List<DeviceRule> findByShortUrlId(UUID shortUrlId) ;
}