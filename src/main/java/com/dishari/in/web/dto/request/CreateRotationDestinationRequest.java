package com.dishari.in.web.dto.request;

import java.time.LocalTime;

public record CreateRotationDestinationRequest(
        String destinationUrl ,
        int weight ,
        LocalTime activeFrom ,
        LocalTime activeTo ,
        int position ,
        boolean active
) {}
