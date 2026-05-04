package com.dishari.in.domain.entity;

import com.dishari.in.domain.enums.DeviceType;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.Instant;
import java.util.UUID;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
@Getter
@Setter
@Table(name = "click_events", indexes = {
        @Index(name = "idx_click_short_url", columnList = "short_url_id"),
        @Index(name = "idx_click_clicked_at", columnList = "clicked_at"),
        @Index(name = "idx_click_short_url_clicked_at", columnList = "short_url_id,clicked_at"),
        @Index(name = "idx_click_country", columnList = "country"),
        @Index(name = "idx_click_device", columnList = "device")
})
public class ClickEvent extends BaseEntity{

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "short_url_id" , nullable = false , updatable = false)
    private ShortUrl shortUrl ;

    @Column(name = "ip_hash" , nullable = false , length = 64)
    private String ipHash ;  // SHA-256 hashed for privacy

    @Column(name = "user_agent" , nullable = false , length = 512)
    private String userAgent ;

    @Column(name = "browser" , nullable = false)
    private String browser ;

    @Column(name = "os" , nullable = false)
    private String os ;

    @Column(name = "device" , nullable = false)
    @Enumerated(EnumType.STRING)
    private DeviceType device ;

    @Column(name = "country" , nullable = false , length = 2)
    private String country ;

    @Column(name = "city" , nullable = false)
    private String city ;

    @Column(name = "referer_domain", length = 255)
    private String refererDomain;   // pre-extracted domain

    @Column(name = "is_unique", nullable = false)
    @Builder.Default
    private boolean unique = false;  // Bloom filter result

    @Column(name = "variant_id")
    private UUID variantId ;

    @Column(name = "clicked_at" , nullable = false , updatable = false)
    private Instant clickedAt ;
}
