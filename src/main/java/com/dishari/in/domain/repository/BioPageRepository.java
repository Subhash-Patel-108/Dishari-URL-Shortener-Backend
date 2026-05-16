package com.dishari.in.domain.repository;

import com.dishari.in.domain.entity.BioPage;
import com.dishari.in.web.projection.BioPageProjection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
public interface BioPageRepository extends JpaRepository<BioPage, UUID> {

    // ── Existence checks ─────────────────────────────────────────
    boolean existsByHandleAndDeletedAtIsNull(String handle);

    // ── Lookups ──────────────────────────────────────────────────
    Optional<BioPage> findByHandleAndDeletedAtIsNull(String handle);

    Optional<BioPage> findByIdAndDeletedAtIsNull(UUID id);

    // ── Fetch with links in one query ────────────────────────────
    @Query("SELECT bp FROM BioPage bp " +
            "LEFT JOIN FETCH bp.links bl " +
            "WHERE bp.handle = :handle " +
            "AND bp.deletedAt IS NULL " +
            "AND (bl IS NULL OR bl.deletedAt IS NULL) " +
            "ORDER BY bl.position ASC")
    Optional<BioPage> findByHandleWithLinks(
            @Param("handle") String handle);

    @Query("SELECT bp FROM BioPage bp " +
            "LEFT JOIN FETCH bp.links bl " +
            "WHERE bp.id = :id " +
            "AND bp.deletedAt IS NULL " +
            "AND (bl IS NULL OR bl.deletedAt IS NULL) " +
            "ORDER BY bl.position ASC")
    Optional<BioPage> findByIdWithLinks(
            @Param("id") UUID id);

    // ── User's bio pages ─────────────────────────────────────────
    List<BioPage> findByUserIdAndDeletedAtIsNull(UUID userId);

    // ── Increment view count atomically ──────────────────────────
    @Modifying
    @Query("UPDATE BioPage bp " +
            "SET bp.viewCount = bp.viewCount + 1 " +
            "WHERE bp.id = :id " +
            "AND bp.deletedAt IS NULL")
    int incrementViewCount(@Param("id") UUID id);
}