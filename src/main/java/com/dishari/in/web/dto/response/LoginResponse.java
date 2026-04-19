package com.dishari.in.web.dto.response;

import com.dishari.in.domain.entity.User;
import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@Builder
@NoArgsConstructor

public class LoginResponse {
    private String accessToken ;
    private long expirationInMS ;
    private String tokenType ;
    private UserResponse userResponse ;

    public static LoginResponse fromEntity(String accessToken , long expirationInMS , User user ) {
        return LoginResponse.builder()
                .accessToken(accessToken)
                .expirationInMS(expirationInMS)
                .tokenType("Bearer ")
                .userResponse(UserResponse.fromEntity(user))
                .build();
    }
}
