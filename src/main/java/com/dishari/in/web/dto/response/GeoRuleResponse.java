package com.dishari.in.web.dto.response;

import com.dishari.in.domain.entity.GeoRule;

public record GeoRuleResponse(
        String id ,
        String countryCode ,
        String destinationUrl ,
        int priority ,
        boolean isDefault
) {
    public static GeoRuleResponse fromEntity(GeoRule request) {
        return new GeoRuleResponse(
                request.getId().toString() ,
                request.getCountryCode() ,
                request.getDestinationUrl() ,
                request.getPriority() ,
                request.isDefault()
        );
    }
}
