package com.dishari.in.web.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record TagRequest(
        @NotNull(message = "Tag name is required.")
        @Size(min = 2 , max = 50 , message = "Name must contains between 2 to 50.")
        String name ,

        @NotBlank(message = "Tag color is required.")
        @Size(max = 7 , message = "Color must be in Hexadecimal.")
        String color

) {}
