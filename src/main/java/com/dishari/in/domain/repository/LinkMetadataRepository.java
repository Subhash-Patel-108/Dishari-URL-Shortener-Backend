package com.dishari.in.domain.repository;

import com.dishari.in.domain.entity.LinkMetadata;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface LinkMetadataRepository extends JpaRepository<LinkMetadata, UUID> {
    boolean existsByUrlHash(String urlHash) ;
    Optional<LinkMetadata> findByUrlHash(String hashUrl) ;
}