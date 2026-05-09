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

    public boolean isActiveAt(LocalTime currentTime) {
        if (!active) return false;
        if (activeFrom == null && activeTo == null) return true; // Always active

        return isWithinTimeWindow(currentTime);
    }

    private boolean isWithinTimeWindow(LocalTime now) {
        if (activeFrom == null || activeTo == null || now == null) {
            return false;
        }

        // Normal window: 08:00 to 16:00
        if (!activeFrom.isAfter(activeTo)) {
            return !now.isBefore(activeFrom) && !now.isAfter(activeTo);
        }

        // Overnight window: 22:00 to 06:00
        return !now.isBefore(activeFrom) || !now.isAfter(activeTo);
    }

}
