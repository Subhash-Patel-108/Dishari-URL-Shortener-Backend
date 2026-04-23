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
    // 1. Fetch one LinkRotation with all its destinations in a single query
    @Query("SELECT lr FROM LinkRotation lr " +
            "LEFT JOIN FETCH lr.rotationDestinations " +
            "WHERE lr.shortUrl.id = :shortUrlId")
    Optional<LinkRotation> findByShortUrlIdWithDestinations(@Param("shortUrlId") UUID shortUrlId);

}