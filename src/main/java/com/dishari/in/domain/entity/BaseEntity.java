package com.dishari.in.domain.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

@MappedSuperclass
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public abstract class BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id ;
    
    @Column(name = "updated_at" , nullable = false , columnDefinition = "TIMESTAMP" , updatable = true)
    private Instant updatedAt ;

    @Column(name = "created_at" , nullable = false , updatable = false)
    private Instant createdAt ;

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        this.createdAt = now;   // set on in-memory object BEFORE insert
        this.updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now(); // set on in-memory object BEFORE update
    }

}
