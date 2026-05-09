package com.dishari.in.web.dto.request;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

public record CreateRotationDestinationRequest(
        String destinationUrl ,
        Integer weight ,
        LocalTime activeFrom , // 00:00:00
        LocalTime activeTo , // - 23:59:59
        Integer position ,
        Boolean active
) {}
