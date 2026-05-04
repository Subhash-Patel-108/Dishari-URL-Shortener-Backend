package com.dishari.in.domain.repository;

import com.dishari.in.domain.entity.ShortUrl;
import com.dishari.in.domain.entity.User;
import com.dishari.in.domain.enums.UrlStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

public interface ShortUrlRepository extends JpaRepository<ShortUrl, UUID> , JpaSpecificationExecutor<ShortUrl> {
    boolean existsBySlugAndStatusAndDeletedAtIsNull(String slug, UrlStatus status);

    @Query("SELECT COUNT(su) > 0 FROM ShortUrl su WHERE su.slug = :slug AND su.deletedAt IS NULL")
    boolean existsBySlugAndDeletedAtIsNull(@Param("slug") String slug);

    // 2. Standard Query Methods (JPA automatically handles @SoftDelete here)
    Optional<ShortUrl> findByUserAndOriginalUrlAndStatusAndDeletedAtIsNull(User user, String originalUrl, UrlStatus status);

    // 3. Custom Query: Must explicitly check for null deletedAt
    @Query("SELECT su FROM ShortUrl su " +
            "LEFT JOIN FETCH su.tags " +
            "WHERE su.id = :id AND su.deletedAt IS NULL")
    Optional<ShortUrl> findByIdWithTagsAndDeletedAtIsNull(@Param("id") UUID id);

    @Modifying
    @Transactional
    @Query("UPDATE ShortUrl s SET s.clickCount = s.clickCount + :count " +
            "WHERE s.slug = :slug AND s.deletedAt IS NULL")
    int incrementClickCount(
            @Param("slug") String slug,
            @Param("count") long count);

    Optional<ShortUrl> findByIdAndDeletedAtIsNull(UUID uuid);

    Optional<ShortUrl> findBySlugAndDeletedAtIsNull(String slug);

    @Modifying
    @Transactional
    @Query("UPDATE ShortUrl s SET s.qrCodeUrl = :qrCodeUrl " +
            "WHERE s.id = :id AND s.deletedAt IS NULL")
    int updateQrCodeUrl(
            @Param("id") UUID id,
            @Param("qrCodeUrl") String qrCodeUrl);
}