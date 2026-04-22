package com.dishari.in.domain.entity;

import com.dishari.in.domain.enums.UrlStatus;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.SoftDelete;
import org.hibernate.annotations.SoftDeleteType;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder


@Table(name = "short_urls" , indexes = {
        @Index(name = "short_url_slug_idx" , columnList = "slug" , unique = true) ,
        @Index(name = "short_url_user_idx" , columnList = "user_id") ,
        @Index(name = "short_url_user_status_idx" , columnList = "status,user_id") ,
        @Index(name = "idx_user_url_status", columnList = "user_id, original_url, status") ,
        @Index(name = "idx_slug_status", columnList = "slug, status")
})

public class ShortUrl extends BaseEntity{

    @Column(name = "slug" , unique = true , length = 32 , nullable = false)
    private String slug ;

    @Column(name = "original_url" , length = 2048 , nullable = false)
    private String originalUrl ;

    @Column(name = "title" )
    private String title ;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id" , nullable = false , updatable = false)
    private User user ;

    @Column(name = "expires_at")
    private Instant expiresAt ;

    @Column(name = "max_clicks")
    private Long maxClicks ;

    @Column(name = "click_count" , nullable = false )
    @Builder.Default
    private long clickCount = 0 ;

    @Column(name = "hashed_password")
    private String hashedPassword ;

    @Column(name = "qr_code_url")
    private String qrCodeUrl ;

    @Column(name = "utm_source")
    private String utmSource ;

    @Column(name = "utm_medium" )
    private String utmMedium ;

    @Column(name = "utm_campaign")
    private String utmCampaign ;

    @Column(name = "utm_term" )
    private String utmTerm ;

    @Column(name = "utm_content")
    private String utmContent ;

    @Column(name = "has_geo_rule" )
    @Builder.Default
    private boolean hasGeoRule = false ;

    @Column(name = "has_device_rule" )
    @Builder.Default
    private boolean hasDeviceRule = false ;

    @Column(name = "has_link_rotation" )
    @Builder.Default
    private boolean hasLinkRotation = false ;

    @Enumerated(EnumType.STRING)
    @Column(name = "status" , nullable = false)
    @Builder.Default
    private UrlStatus status = UrlStatus.ACTIVE;

    @Column(name = "flagged" , nullable = false )
    @Builder.Default
    private boolean flagged = false ;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "short_url_tags" ,
            joinColumns = @JoinColumn(name = "short_url_id" , nullable = false) ,
            inverseJoinColumns = @JoinColumn(name = "tag_id" , nullable = false))
    @Builder.Default
    Set<Tag> tags = new HashSet<>() ;

    @Version
    private Long version ;

    @SoftDelete(strategy = SoftDeleteType.TIMESTAMP)
    private Instant deletedAt ;
}
