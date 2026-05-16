package com.dishari.in.domain.repository;

import com.dishari.in.domain.entity.Workspace;
import com.dishari.in.web.projection.WorkspaceSummaryProjection;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface WorkspaceRepository extends JpaRepository<Workspace, UUID> {

    Optional<Workspace> findBySlug(String slug);

    boolean existsBySlug(String slug);

    @Query("SELECT w FROM Workspace w " +
            "JOIN WorkspaceMember wm ON w.id = wm.workspace.id " +
            "WHERE wm.user.id = :userId AND wm.status = 'ACTIVE' " +
            "AND w.deletedAt IS NULL " +
            "ORDER BY w.createdAt DESC")
    List<Workspace> findActiveByMemberUserId(@Param("userId") UUID userId);


    @Query("SELECT w FROM Workspace w " +
            "JOIN WorkspaceMember wm ON w.id = wm.workspace.id " +
            "WHERE wm.user.id = :userId AND wm.role = :role AND wm.status = 'ACTIVE' " +
            "AND w.deletedAt IS NULL " +
            "ORDER BY w.createdAt DESC")
    List<Workspace> findByMemberUserIdAndRole(@Param("userId") UUID userId,
                                              @Param("role") String role);

    @Query("""
        SELECT 
        w.id as id, w.name as name, w.slug as slug, w.logoUrl as logoUrl, 
        w.plan as plan, w.personal as personal, w.createdAt as createdAt,
        w.linkCount as linkCount, 
        wm.role as currentUserRole,
        (SELECT COUNT(m) FROM WorkspaceMember m WHERE m.workspace.id = w.id) as memberCount
            FROM Workspace w
            JOIN WorkspaceMember wm ON wm.workspace.id = w.id
            WHERE wm.user.id = :userId
            AND (:search IS NULL OR 
                 LOWER(w.name) LIKE LOWER(CONCAT('%', :search, '%')) OR 
                 LOWER(w.slug) LIKE LOWER(CONCAT('%', :search, '%')) OR
                 LOWER(w.description) LIKE LOWER(CONCAT('%', :search, '%')))
        """)
    Page<WorkspaceSummaryProjection> findSummariesByUserId(
            @Param("userId") UUID userId,
            @Param("search") String search,
            Pageable pageable);

    @Query("SELECT COUNT(w) FROM Workspace w " +
            "JOIN WorkspaceMember wm ON w.id = wm.workspace.id " +
            "WHERE wm.user.id = :userId AND wm.status = 'ACTIVE' " +
            "AND w.deletedAt IS NULL")
    long countActiveByMemberUserId(@Param("userId") UUID userId);

    @Query("SELECT w FROM Workspace w WHERE w.id = :id AND w.deletedAt IS NULL")
    Optional<Workspace> findActiveById(@Param("id") UUID id);

    @Query("SELECT w FROM Workspace w WHERE w.owner.id = :ownerId AND w.deletedAt IS NULL")
    List<Workspace> findActiveByOwnerId(@Param("ownerId") UUID ownerId);

    @Transactional
    @Modifying
    @Query("""
            UPDATE Workspace w
            SET w.deletedAt = CURRENT_TIMESTAMP
            WHERE w.id = :workspaceId
        """)
    void softDelete(@Param("workspaceId") UUID workspaceId);

    @Modifying
    @Query("UPDATE Workspace w SET w.linkCount = w.linkCount + 1 WHERE w.id = :workspaceId")
    void incrementLinkCount(@Param("workspaceId") UUID workspaceId);

    @Modifying
    @Query("UPDATE Workspace w SET w.linkCount = w.linkCount - 1 WHERE w.id = :workspaceId AND w.linkCount > 0")
    void decrementLinkCount(@Param("workspaceId") UUID workspaceId);
}