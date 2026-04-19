package com.dishari.in.web.dto.response;

import lombok.*;

import java.time.Instant;
import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder

public class MessageResponse {
    private Instant timestamp;
    private String message;
    private int status;
    private String path;
}
