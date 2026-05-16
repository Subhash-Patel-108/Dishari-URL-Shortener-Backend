package com.dishari.in.domain.repository;

import com.dishari.in.domain.entity.BioLink;
import com.dishari.in.web.projection.BioLinkProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BioLinkRepository extends JpaRepository<BioLink, UUID> {

    List<BioLink> findByBioPageIdAndDeletedAtIsNullOrderByPositionAsc(
            UUID bioPageId);

    Optional<BioLink> findByIdAndDeletedAtIsNull(UUID id);

    // Max position in a bio page — for appending new links
    @Query("SELECT COALESCE(MAX(bl.position), 0) " +
            "FROM BioLink bl " +
            "WHERE bl.bioPage.id = :bioPageId " +
            "AND bl.deletedAt IS NULL")
    int findMaxPositionByBioPageId(
            @Param("bioPageId") UUID bioPageId);

    // Increment click count
    @Modifying
    @Query("UPDATE BioLink bl " +
            "SET bl.clickCount = bl.clickCount + 1 " +
            "WHERE bl.id = :id " +
            "AND bl.deletedAt IS NULL")
    int incrementClickCount(@Param("id") UUID id);

    // Count active links in a page
    @Query("SELECT COUNT(bl) FROM BioLink bl " +
            "WHERE bl.bioPage.id = :bioPageId " +
            "AND bl.deletedAt IS NULL")
    int countByBioPageId(@Param("bioPageId") UUID bioPageId);
}