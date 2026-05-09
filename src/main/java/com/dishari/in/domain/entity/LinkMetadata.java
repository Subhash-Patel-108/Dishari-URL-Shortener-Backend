package com.dishari.in.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.*;

import java.time.Instant;

@Entity
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Table(
        name = "link_metadata" , indexes = {
                @Index(name = "idx_url_hash", columnList = "url_hash", unique = true)
        }
)
public class LinkMetadata extends BaseEntity{

    @Column(name = "url_hash", unique = true, nullable = false, length = 64)
    private String urlHash; // SHA-256 of the destination URL

    @Column(length = 255)
    private String title;

    @Column(length = 500)
    private String description;

    @Column(name = "image_url")
    private String imageUrl;

    @Column(name = "favicon_url")
    private String faviconUrl;

    @Column(name = "site_name", length = 100)
    private String siteName;

    @Column(length = 100)
    private String author;

    @Column(name = "canonical_url")
    private String canonicalUrl;

    @Column(name = "destination_type" , length = 50)
    private String destinationType;  // High-level type: video, news, ecommerce, social_media, blog,
    @Column(name = "content_type", length = 50)
    private String contentType;     // article, video, website...

    @Column(name = "last_scraped_at")
    private Instant lastScrapedAt;
}
