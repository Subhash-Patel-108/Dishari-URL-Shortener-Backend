package com.dishari.in.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.Instant;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder

@Table(name = "bio_link_configs" , indexes = {
        @Index(name = "bio_link_config_bio_link_id_idx" , columnList = "bio_link_id" , unique = true) ,
})
public class BioLinkConfig extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bio_link_id" , nullable = false , updatable = false)
    private BioLink bioLink ;

    @Column(name = "hashed_password")
    private String hashedPassword ;

    @Column(name = "expires_at")
    private Instant expiresAt ;

    @Column(name = "max_clicks")
    private Long maxClicks ;

    @Column(name = "email_alerts_enable")
    @Builder.Default
    private boolean emailAlertsEnable = false ;

    @Version
    private Long version ;
}
