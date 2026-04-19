package com.dishari.in.domain.entity;

import com.dishari.in.domain.enums.CustomDomainStatus;
import com.dishari.in.domain.enums.VerificationType;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.Instant;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
@Entity
@Table(name = "custom_domains" , indexes = {
    @Index(name = "idx_custom_domain_lookup", columnList = "domain, workspace_id"),
    @Index(name = "idx_custom_domain_workspace", columnList = "workspace_id")
})
public class CustomDomain extends BaseEntity{
    @Column(name = "domain" , nullable = false , updatable = false , unique = true)
    private String domain ;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workspace_id" , nullable = false , updatable = false)
    private Workspace workspace ;

    @Column(name = "root_redirect" , length = 2048)
    private String rootRedirect ;

    @Column(name = "error_redirect" , length = 2048)
    private String errorRedirect ;

    @Column(name = "verified" , nullable = false)
    private boolean verified ;

    @Column(name = "verification_token")
    private String verificationToken ;

    @Enumerated(EnumType.STRING)
    @Column(name = "verification_type")
    private VerificationType verificationType ;

    @Enumerated(EnumType.STRING)
    @Column(name = "status" , nullable = false)
    private CustomDomainStatus status ;

    @Column(name = "ssl_enabled" , nullable = false)
    private boolean sslEnabled ;

    @Column(name = "verified_at")
    private Instant verifiedAt ;
}
