package com.dishari.in.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.SoftDelete;
import org.hibernate.annotations.SoftDeleteType;

import java.time.Instant;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder

@Table(name = "bio_links" , indexes = {
        @Index(name = "bio_link_title_idx" , columnList = "title" ) ,
        @Index(name = "bio_link_bio_page_id_idx" , columnList = "bio_page_id") ,
        @Index(name = "bio_link_position_idx" , columnList = "position")
})
public class BioLink extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bio_page_id" , nullable = false , updatable = false)
    private BioPage bioPage ;

    @Column(name = "title" , nullable = false , length = 100)
    private String title ;

    @Column(name = "url" , nullable = false , length = 2048)
    private String url ;

    @Column(name = "icon_type" , length = 50)
    private String iconType ;

    @Column(name = "position" , nullable = false)
    private int position ;

    @Column(name = "click_count" , nullable = false)
    private long clickCount ;

    @Column(name = "is_active" , nullable = false)
    @Builder.Default
    private boolean isActive = true ;

    @Column(name = "has_advanced_config")
    @Builder.Default
    private boolean hasAdvancedConfig = false ;

    @Version
    private Long version ;

    private Instant deletedAt ;
}
