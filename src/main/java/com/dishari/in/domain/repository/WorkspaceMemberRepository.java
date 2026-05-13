package com.dishari.in.domain.repository;

import com.dishari.in.domain.entity.WorkspaceMember;
import com.dishari.in.domain.enums.WorkspaceMemberStatus;
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
public interface WorkspaceMemberRepository extends JpaRepository<WorkspaceMember, UUID> {

    @Query("SELECT wm FROM WorkspaceMember wm " +
            "WHERE wm.workspace.id = :workspaceId AND wm.user.id = :userId " +
            "AND wm.removedAt IS NULL")
    Optional<WorkspaceMember> findActiveByWorkspaceIdAndUserId(
            @Param("workspaceId") UUID workspaceId,
            @Param("userId") UUID userId);

    @Query("SELECT wm FROM WorkspaceMember wm " +
            "WHERE wm.id = :memberId AND wm.removedAt IS NULL")
    Optional<WorkspaceMember> findActiveById(@Param("memberId") UUID memberId);

    @Query("""
        SELECT wm FROM WorkspaceMember wm 
        JOIN FETCH wm.user u
        LEFT JOIN FETCH wm.invitedBy 
        WHERE wm.workspace.id = :workspaceId 
        AND wm.removedAt IS NULL 
        AND (:search IS NULL OR 
             LOWER(u.name) LIKE LOWER(CONCAT('%', :search, '%')) OR 
             LOWER(u.email) LIKE LOWER(CONCAT('%', :search, '%')))
    """)
    Page<WorkspaceMember> findActiveByWorkspaceId(
            @Param("workspaceId") UUID workspaceId,
            @Param("search") String search,
            Pageable pageable
    );

    @Query("SELECT wm FROM WorkspaceMember wm " +
            "WHERE wm.workspace.id = :workspaceId AND wm.removedAt IS NULL " +
            "ORDER BY wm.role ASC, wm.joinedAt ASC")
    List<WorkspaceMember> findActiveByWorkspaceId(@Param("workspaceId") UUID workspaceId);

    @Query("SELECT wm FROM WorkspaceMember wm " +
            "WHERE wm.workspace.id = :workspaceId AND wm.status = :status " +
            "AND wm.removedAt IS NULL")
    List<WorkspaceMember> findByWorkspaceIdAndStatus(
            @Param("workspaceId") UUID workspaceId,
            @Param("status") WorkspaceMemberStatus status);

    @Query("SELECT COUNT(wm) FROM WorkspaceMember wm " +
            "WHERE wm.workspace.id = :workspaceId AND wm.status = 'ACTIVE' " +
            "AND wm.removedAt IS NULL")
    long countActiveByWorkspaceId(@Param("workspaceId") UUID workspaceId);

    @Query("SELECT wm FROM WorkspaceMember wm " +
            "WHERE wm.user.id = :userId AND wm.status = 'ACTIVE' " +
            "AND wm.removedAt IS NULL")
    List<WorkspaceMember> findActiveWorkspacesByUserId(@Param("userId") UUID userId);

    @Query("SELECT CASE WHEN COUNT(wm) > 0 THEN true ELSE false END " +
            "FROM WorkspaceMember wm " +
            "WHERE wm.workspace.id = :workspaceId AND wm.user.id = :userId " +
            "AND wm.removedAt IS NULL")
    boolean existsActiveByWorkspaceIdAndUserId(
            @Param("workspaceId") UUID workspaceId,
            @Param("userId") UUID userId);

    @Query("SELECT wm FROM WorkspaceMember wm " +
            "WHERE wm.workspace.id = :workspaceId AND wm.role = 'OWNER' " +
            "AND wm.removedAt IS NULL")
    Optional<WorkspaceMember> findOwnerByWorkspaceId(@Param("workspaceId") UUID workspaceId);

    @Query("SELECT COUNT(wm) FROM WorkspaceMember wm " +
            "WHERE wm.workspace.id = :workspaceId AND wm.removedAt IS NULL")
    long countAllByWorkspaceId(@Param("workspaceId") UUID workspaceId);

    @Transactional
    @Modifying
    @Query("UPDATE WorkspaceMember wm SET wm.removedAt = CURRENT_TIMESTAMP " +
            "WHERE wm.id = :memberId AND wm.removedAt IS NULL")
    int softDeleteMember(@Param("memberId") UUID memberId);
}