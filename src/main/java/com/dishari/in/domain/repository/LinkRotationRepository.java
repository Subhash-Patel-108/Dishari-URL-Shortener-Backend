package com.dishari.in.domain.repository;

import com.dishari.in.domain.entity.LinkRotation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface LinkRotationRepository extends JpaRepository<LinkRotation, UUID> {
    @Query("SELECT lr FROM LinkRotation lr " +
            "LEFT JOIN FETCH lr.rotationDestinations rd " +
            "WHERE lr.shortUrl.id = :shortUrlId " +
            "AND (rd.active = true OR rd IS NULL) " +
            "ORDER BY rd.position ASC")
    Optional<LinkRotation> findActiveRotationWithDestinations(@Param("shortUrlId") UUID shortUrlId);

}