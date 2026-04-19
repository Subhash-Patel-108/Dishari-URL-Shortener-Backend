package com.dishari.in.domain.entity;


import com.dishari.in.domain.enums.DeviceType;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder

@Table(name = "device_rule" , indexes = {
        @Index(name = "idx_device_rule_short_url_id_device_type" , columnList = "short_url_id , device_type")

})
public class DeviceRule extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "short_url_id" , nullable = false , updatable = false)
    private ShortUrl shortUrl ;

    @Enumerated(EnumType.STRING)
    @Column(name = "device_type" , nullable = false)
    private DeviceType deviceType ;

    @Column(name = "destination_url" , nullable = false , length = 2048)
    private String destinationUrl ;

    @Column(name = "is_default")
    @Builder.Default
    private boolean isDefault = false ;

}
