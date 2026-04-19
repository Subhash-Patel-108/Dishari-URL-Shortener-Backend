package com.dishari.in.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder

@Table(name = "geo_rules" , indexes = {
        @Index(name = "geo_rule_short_url_id_idx" , columnList = "short_url_id") ,
        @Index(name = "geo_rule_country_code_idx" , columnList = "country_code") ,
        @Index(name = "geo_rule_priority" , columnList = "priority")
})
public class GeoRule extends BaseEntity{
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "short_url_id" , nullable = false , updatable = false)
    private ShortUrl shortUrl ;

    @Column(name = "country_code" , nullable = false , length = 2)
    private String countryCode ;

    @Column(name = "destination_url" , nullable = false , length = 2048)
    private String destinationUrl ;

    @Column(name = "priority")
    @Builder.Default
    private int priority = 1 ; // by default every one has priority 1

    @Builder.Default
    @Column(name = "is_default" , nullable = false)
    private boolean isDefault = false ;
}
