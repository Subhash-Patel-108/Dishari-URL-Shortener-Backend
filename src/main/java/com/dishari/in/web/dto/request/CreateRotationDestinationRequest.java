package com.dishari.in.web.dto.request;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

public record CreateRotationDestinationRequest(
        String destinationUrl ,
        int weight ,
        LocalTime activeFrom ,
        LocalTime activeTo ,
        int position ,
        boolean active
) {}
