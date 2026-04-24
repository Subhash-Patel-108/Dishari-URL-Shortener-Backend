package com.dishari.in.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.validator.constraints.Range;

import java.time.Instant;
import java.time.LocalTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder

@Entity
@Table(name = "rotation_destinations" , indexes = {
        @Index(name = "idx_rot_dest_lookup", columnList = "link_rotation_id, active, position")
})
public class RotationDestination extends BaseEntity{
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "link_rotation_id" , nullable = false , updatable = false)
    private LinkRotation linkRotation ;

    @Column(name = "destination_url" , nullable = false , length = 2048)
    private String destinationUrl ;

    @Column(name = "weight")
    @Range(min = 1 , max = 100)
    private Integer weight ;

    @Column(name = "active_from")
    private LocalTime activeFrom ; //"00:00"

    @Column(name = "active_to" )
    private LocalTime activeTo ;

    @Column(name = "position")
    private Integer position ;

    @Column(name = "active")
    @Builder.Default
    private boolean active = true ;

}
