package com.dishari.in.domain.repository;

import com.dishari.in.domain.entity.ShortUrl;
import com.dishari.in.domain.entity.User;
import com.dishari.in.domain.enums.UrlStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ShortUrlRepository extends JpaRepository<ShortUrl, UUID> {
    // Efficiently returns a boolean without fetching entity data
    boolean existsBySlugAndStatus(String slug, UrlStatus status);

    // Existing methods
    boolean existsBySlug(String slug);
    Optional<ShortUrl> findByUserAndOriginalUrlAndStatus(User user, String originalUrl, UrlStatus status);
}