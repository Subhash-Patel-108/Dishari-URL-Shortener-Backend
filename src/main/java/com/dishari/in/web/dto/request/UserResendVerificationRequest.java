package com.dishari.in.web.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder

public class UserResendVerificationRequest {
    @NotBlank(message = "Email is required")
    @Email
    private String email ;
}
