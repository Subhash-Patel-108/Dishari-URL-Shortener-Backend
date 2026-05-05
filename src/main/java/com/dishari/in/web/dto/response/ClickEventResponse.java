package com.dishari.in.web.dto.response;

import com.dishari.in.domain.entity.ClickEvent;

import java.time.Instant;

public record ClickEventResponse(
        String id,
        String clientHash,
        String userAgent,
        String referrer,
        Instant clickedAt,
        String country,
        String device
) {
    public static ClickEventResponse fromEntity(ClickEvent clickEvent) {
        return new ClickEventResponse(
                clickEvent.getId().toString() ,
                clickEvent.getIpHash() ,
                clickEvent.getUserAgent() ,
                clickEvent.getRefererDomain() ,
                clickEvent.getClickedAt() ,
                clickEvent.getCountry() ,
                clickEvent.getDevice().toString()
        ) ;
    }
}
