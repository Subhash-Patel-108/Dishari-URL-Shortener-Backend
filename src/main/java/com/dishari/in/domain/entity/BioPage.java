package com.dishari.in.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.SoftDelete;
import org.hibernate.annotations.SoftDeleteType;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder

@Table(name = "bio_pages" , indexes = {
        @Index(name = "bio_page_handle_idx" , columnList = "handle" ) ,
        @Index(name = "bio_page_user_id_idx" , columnList = "user_id")
})
public class BioPage extends BaseEntity {

    @Column(name = "handle" , nullable = false , length = 32)
    private String handle ;//a unique identifier

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id" , nullable = false , updatable = false)
    private User user ;

    @Column(name = "display_name" , length = 128)
    private String displayName ;

    @Column(name = "bio" , length = 1024)
    private String bio ;

    @Column(name = "avatar_url" , length = 1024)
    private String avatarUrl ;

    @Column(name = "view_count" , nullable = false)
    @Builder.Default
    private Long viewCount = 0L ;

    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true ;

    @Version
    private Long version ;

    private Instant deletedAt ;

    // Back-reference to links
    @OneToMany(mappedBy = "bioPage",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY)
    @OrderBy("position ASC")
    @Builder.Default
    private List<BioLink> links = new ArrayList<>();
}
