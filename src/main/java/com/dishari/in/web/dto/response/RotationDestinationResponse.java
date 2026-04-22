package com.dishari.in.web.dto.response;

import com.dishari.in.domain.entity.RotationDestination;

import java.time.LocalTime;

public record RotationDestinationResponse(
        String id ,
        String destinationUrl ,
        int weight ,
        LocalTime activeFrom ,
        LocalTime activeTo ,
        int position ,
        boolean active
) {

    public static RotationDestinationResponse fromEntity(RotationDestination request) {
        return new RotationDestinationResponse(
                request.getId().toString() ,
                request.getDestinationUrl(),
                request.getWeight() ,
                request.getActiveFrom() ,
                request.getActiveTo() ,
                request.getPosition() ,
                request.isActive()
        );
    }
}
