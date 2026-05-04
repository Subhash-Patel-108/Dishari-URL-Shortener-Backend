package com.dishari.in.domain.entity;

import com.dishari.in.domain.enums.Plan;
import com.dishari.in.domain.enums.SocialProvider;
import com.dishari.in.domain.enums.UserRole;
import com.dishari.in.domain.enums.UserStatus;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.Instant;
import java.util.Collection;
import java.util.List;

@Entity
@Getter
@Setter
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor

@Table(name = "users" , indexes = {
        @Index(name = "user_email_idx" , columnList = "email" , unique = true) ,
        @Index(name = "user_status_idx" , columnList = "status")
})

public class User extends BaseEntity implements UserDetails {

    @Column(name = "email" , nullable = false , unique = true , length = 254)
    private String email ;

    @Column(name = "name" , nullable = false , length = 30)
    private String name ;

    @Column(name = "hashed_password")
    private String hashedPassword;

    @Enumerated(EnumType.STRING)
    @Column(name = "role" , nullable = false)
    @Builder.Default
    private UserRole role = UserRole.ROLE_USER;

    @Enumerated(EnumType.STRING)
    @Column(name = "status" , nullable = false)
    @Builder.Default
    private UserStatus status = UserStatus.VERIFICATION_PENDING ;

    @Enumerated(EnumType.STRING)
    @Column(name = "social_provider" , nullable = false)
    @Builder.Default
    private SocialProvider socialProvider = SocialProvider.LOCAL ;

    @Column(name = "verified" , nullable = false)
    @Builder.Default
    private boolean verified = false ;

    @Column(name = "enabled", nullable = false )
    @Builder.Default
    private boolean enabled = false ;

    @Column(name = "frozen" , nullable = false)
    @Builder.Default
    private boolean frozen = false ;

    @Column(name = "frozen_reason" )
    private String frozenReason ;

    @Column(name = "frozen_at" )
    private Instant frozenAt ;

    @Enumerated(EnumType.STRING)
    @Column(name = "plan" , nullable = false)
    @Builder.Default
    private Plan plan = Plan.FREE ;

    @Column(name = "has_premium" )
    @Builder.Default
    private boolean hasPremium = false ;

    @Column(name = "plan_expired_at")
    private Instant planExpiry ;

    @Column(name = "time_zone" , nullable = false)
    @Builder.Default
    private String timeZone = "UTC" ;

    @Column(name = "avatar_url" , length = 500)
    private String avatarUrl ;

    @Column(name = "deleted_at")
    private Instant deletedAt ;

    @Override
    @NullMarked
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority(role.name()));
    }

    @Override
    public @Nullable String getPassword() {
        return this.hashedPassword;
    }

    @Override
    @NullMarked
    public String getUsername() {
        return this.email ;// Spring Security principal identifier — intentionally email
    }


    @Override
    public boolean isAccountNonLocked() {
        return status != UserStatus.BANNED && status != UserStatus.FROZEN;
    }

    @Override
    public boolean isEnabled() {
        return status == UserStatus.ACTIVE;
    }
}
