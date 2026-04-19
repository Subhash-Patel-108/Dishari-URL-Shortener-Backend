package com.dishari.in.domain.entity;

import com.dishari.in.domain.enums.RotationStrategy;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
@Entity
@Table(name = "link_rotations" , indexes = {
        @Index(name = "idx_link_rotation_short_url_id" , columnList = "short_url_id" , unique = true)
})
public class LinkRotation extends BaseEntity{
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "short_url_id" , nullable = false , updatable = false)
    private ShortUrl shortUrl ;

    @Enumerated(EnumType.STRING)
    @Column(name = "rotation_strategy" , nullable = false )
    private RotationStrategy rotationStrategy ;

    @OneToMany(
            mappedBy = "linkRotation" ,
            fetch = FetchType.LAZY ,
            cascade = CascadeType.ALL ,
            orphanRemoval = true)
    @Builder.Default
    private List<RotationDestination> rotationDestinations = new ArrayList<>() ;

    public void addDestination(RotationDestination destination) {
        rotationDestinations.add(destination);
        destination.setLinkRotation(this);
    }
}
