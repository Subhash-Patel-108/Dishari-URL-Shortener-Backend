package com.dishari.in.web.dto.request;

import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder

public class UserUpdateRequest {
    @Size(max = 30 , message = "Username must be less than 30 characters")
    private String name ;
    private String timeZone ;

    @Size(max = 500 , message = "Avatar URL must be less than 500 characters")
    private String avatarUrl ; //TODO: We want to implement it using multipart file and store in cloud storage
}
