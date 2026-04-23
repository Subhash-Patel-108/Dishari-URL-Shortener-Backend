package com.dishari.in.web.dto.response;

import com.dishari.in.domain.entity.DeviceRule;
import com.dishari.in.domain.enums.DeviceType;
import com.dishari.in.web.dto.request.CreateDeviceRuleRequest;

public record DeviceRuleResponse(
        String id ,
        DeviceType deviceType ,
        String destinationUrl ,
        boolean isDefault
) {
    public static DeviceRuleResponse fromEntity(DeviceRule request) {
        return new DeviceRuleResponse(
                request.getId().toString() ,
                request.getDeviceType() ,
                request.getDestinationUrl() ,
                request.isDefault()
        );
    }
}
