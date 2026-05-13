package com.dishari.in.domain.entity;

import com.dishari.in.domain.enums.Plan;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.SoftDelete;
import org.hibernate.annotations.SoftDeleteType;

import java.time.Instant;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder


@Table(name = "workspaces", indexes = {
        @Index(name = "idx_workspace_slug", columnList = "slug", unique = true),
        @Index(name = "idx_workspace_owner", columnList = "owner_id") ,
        @Index(name = "idx_workspace_active", columnList = "active")
})
@Entity
public class Workspace extends BaseEntity {

    @Column(name = "name" , length = 100 , nullable = false)
    private String name ;

    @Column(name = "slug" , nullable = false , updatable = false , length = 50)
    private String slug ;

    @Column(name = "description" , length = 500)
    private String description ;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id" , nullable = false , updatable = false)
    private User owner ;

    @Enumerated(EnumType.STRING)
    @Column(name = "plan" , nullable = false)
    @Builder.Default
    private Plan plan = Plan.FREE ;

    @Column(name = "is_personal")
    @Builder.Default
    private boolean personal = false ;

    // --- Branding & Customization ---
    @Column(name = "logo_url" , length = 2048)
    private String logoUrl;

    @Column(name = "brand_color", length = 7)
    private String brandColor; // Hex code, e.g., #FF5733

    // --- Governance ---
    @Column(name = "is_enabled" , nullable = false)
    @Builder.Default
    private boolean enabled = true ;

    @Column(name = "link_count")
    @Builder.Default
    private long linkCount = 0;

    @Version
    private Long version ;

    private Instant deletedAt ;
}
