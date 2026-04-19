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


@Table(name = "tags" , indexes = {
        @Index(name = "tag_name_idx" , columnList = "name" , unique = true)
})
public class Tag extends BaseEntity {

    @Column(name = "name" , nullable = false , length = 50)
    private String name ;

    @Column(name = "color" , nullable = false , length = 7)
    private String color ;
}
