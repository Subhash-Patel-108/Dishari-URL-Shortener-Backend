package com.dishari.in.domain.repository;

import com.dishari.in.domain.entity.CustomDomain;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CustomDomainRepository extends JpaRepository<CustomDomain, UUID> {
    Optional<CustomDomain> findByDomain(String domain);

    boolean existsByDomain(String domain);

    @Query("SELECT cd FROM CustomDomain cd " +
            "WHERE cd.workspace.id = :workspaceId")
    List<CustomDomain> findByWorkspaceId(@Param("workspaceId") UUID workspaceId);

    @Query("SELECT cd FROM CustomDomain cd " +
            "WHERE cd.workspace.id = :workspaceId AND cd.verified = true")
    List<CustomDomain> findVerifiedByWorkspaceId(@Param("workspaceId") UUID workspaceId);

    @Query("SELECT COUNT(cd) FROM CustomDomain cd " +
            "WHERE cd.workspace.id = :workspaceId")
    long countByWorkspaceId(@Param("workspaceId") UUID workspaceId);

    @Query("SELECT cd FROM CustomDomain cd " +
            "WHERE cd.domain = :domain AND cd.workspace.id = :workspaceId")
    Optional<CustomDomain> findByDomainAndWorkspaceId(
            @Param("domain") String domain,
            @Param("workspaceId") UUID workspaceId);

}