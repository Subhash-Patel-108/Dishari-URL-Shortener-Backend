package com.dishari.in.infrastructure.messaging.event;

import jakarta.servlet.http.HttpServletRequest;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ClickEventGenerationEvent {
   private String shortUrlId;
   private String ipAddress;
   private String userAgent;
   private String referer;
   private UUID variantId ;
   private Instant occurredAt;
}
