package com.dishari.in.domain.repository;

import com.dishari.in.domain.entity.ShortUrl;
import com.dishari.in.domain.entity.User;
import com.dishari.in.domain.enums.UrlStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ShortUrlRepository extends JpaRepository<ShortUrl, UUID> {
    boolean existsBySlug(String slug);

    //Method to check that user has already created the short url for the original long url
    Optional<ShortUrl> findByUserAndOriginalUrlAndStatus(User user, String originalUrl, UrlStatus status);

    //Method to check that user has already created the short url for the original long url
    Optional<ShortUrl> findBySlugAndStatus(String slug, UrlStatus status);
}