package com.dishari.in.web.dto.response;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder

public class RefreshTokenResponse {
    private String accessToken;
    private long accessTokenExpiration;
}
