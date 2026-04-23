package com.dishari.in.web.dto.request;

import com.dishari.in.domain.entity.RotationDestination;
import com.dishari.in.domain.enums.RotationStrategy;

import java.util.Set;

public record CreateLinkRotationRequest(
        RotationStrategy rotationStrategy ,
        Set<CreateRotationDestinationRequest> rotationDestinations
) {
}
