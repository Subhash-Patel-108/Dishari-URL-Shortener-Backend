package com.dishari.in.web.dto.response;

import com.dishari.in.domain.entity.User;
import com.dishari.in.domain.enums.Plan;
import com.dishari.in.domain.enums.SocialProvider;
import com.dishari.in.domain.enums.UserRole;
import com.dishari.in.domain.enums.UserStatus;
import jakarta.persistence.Column;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.validation.constraints.NegativeOrZero;
import lombok.*;

import java.time.Instant;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder

public class UserResponse {
    private String email ;
    private String name ;
    private UserRole role ;
    private UserStatus status ;
    private SocialProvider socialProvider ;
    private boolean verified ;
    private boolean enabled ;
    private boolean frozen ;
    private Plan plan ;
    private String timeZone ;
    private String avatarUrl ;
    private Instant createAt ;
    private Instant updatedAt ;

    public static UserResponse fromEntity(User user) {
        return UserResponse.builder()
                .email(user.getEmail())
                .name(user.getName())
                .role(user.getRole())
                .status(user.getStatus())
                .socialProvider(user.getSocialProvider())
                .verified(user.isVerified())
                .enabled(user.isEnabled())
                .frozen(user.isFrozen())
                .plan(user.getPlan())
                .timeZone(user.getTimeZone())
                .avatarUrl(user.getAvatarUrl())
                .createAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }
}
