package com.dishari.in.web.dto.request;

import jakarta.validation.constraints.*;
import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder

public class UserRegistrationRequest {
    @Email(message = "Invalid email id.")
    @NotBlank(message = "Email is required.")
    private String email ;

    @NotBlank(message = "Username is required." )
    @Size(min = 3 , max = 50 , message = "Name must be between 3 and 50 characters.")
    private String name ;

    @NotBlank(message = "Password is required.")
    @Size(min = 8, max = 128, message = "Password must be between 8 and 128 characters")
    @Pattern(
            regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]+$",
            message = "Password must contain at least one uppercase letter, one lowercase letter, one digit, and one special character"
    )
    private String password ;

    @Size(max = 500 , message = "Url must be less than 500 characters.")
    @NotBlank(message = "Default Url is required.")
    private String avatarUrl ;
}
