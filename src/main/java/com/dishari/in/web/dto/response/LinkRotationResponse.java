package com.dishari.in.web.dto.response;

import com.dishari.in.domain.entity.LinkRotation;
import com.dishari.in.domain.enums.RotationStrategy;

import java.util.List;

public record LinkRotationResponse(
        String id ,
        RotationStrategy rotationStrategy ,
        List<RotationDestinationResponse> rotationDestinations
) {

    public static LinkRotationResponse fromEntity(LinkRotation request) {
        return new LinkRotationResponse(
                request.getId().toString() ,
                request.getRotationStrategy() ,
                request.getRotationDestinations().stream().map(RotationDestinationResponse::fromEntity).toList()
        );
    }
}
