package com.dishari.in.domain.entity;


import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.Instant;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder

@Table(name = "refresh_tokens" , indexes = {
        @Index(name = "refresh_token_id_idx" , columnList = "token_id") ,
        @Index(name = "refresh_token_user_idx" , columnList = "user_id") ,
        @Index(name = "refresh_token_user_revoked_idx", columnList = "user_id, revoked") ,
        @Index(name = "refresh_token_expiry_idx", columnList = "expires_at")
})

public class RefreshToken extends BaseEntity{

    @Column(name = "token_id" , nullable = false , unique = true , length = 100)
    private String tokenId ; // we can't store actual token here, we will store token id (UUID) and Delete the token with in one month if  the token is revoked

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id" , nullable = false , updatable = false)
    private User user ;

    @Column(name = "expires_at" , nullable = false)
    private Instant expiresAt ;

    @Builder.Default
    @Column(name = "revoked" , nullable = false)
    private boolean revoked = false ;

    @Column(name = "user_agent" , nullable = false , length = 255)
    private String userAgent ;

    @Column(name = "ip_address" , nullable = false , length = 64)
    private String ipAddress ;

    @Column(name = "rotate_to_token_id" , length = 100)
    private String rotateToTokenId ;

    @Version
    private Long version ;
}
