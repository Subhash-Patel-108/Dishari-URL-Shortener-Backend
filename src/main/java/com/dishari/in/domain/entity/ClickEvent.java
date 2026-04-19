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
@Table(name = "click_events" , indexes = {
        @Index(name = "click_event_short_url_idx" , columnList = "short_url_id"),
        @Index(name = "click_event_variant_idx" , columnList = "variant_id")
})
public class ClickEvent extends BaseEntity{

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "short_url_id" , nullable = false , updatable = false)
    private ShortUrl shortUrl ;

    @Column(name = "ip_address" , nullable = false , length = 64)
    private String ipAddress ;

    @Column(name = "user_agent" , nullable = false , length = 512)
    private String userAgent ;

    @Column(name = "browser" , nullable = false)
    private String browser ;

    @Column(name = "os" , nullable = false)
    private String os ;

    @Column(name = "device_type" , nullable = false)
    @Enumerated(EnumType.STRING)
    private DeviceType deviceType ;

    @Column(name = "country" , nullable = false , length = 2)
    private String country ;

    @Column(name = "city" , nullable = false)
    private String city ;

    @Column(name = "referer" , length = 512)
    private String referer ;

    @Column(name = "is_unique", nullable = false)
    private boolean isUnique ;

    @Column(name = "variant_id")
    private UUID variantId ;

    @Column(name = "clicked_at" , nullable = false , updatable = false)
    private Instant clickedAt ;
}
