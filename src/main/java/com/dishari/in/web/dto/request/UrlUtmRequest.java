package com.dishari.in.web.dto.request;

public record UrlUtmRequest(
        String utmSource ,
        String utmMedium ,
        String utmCampaign ,
        String utmTerm ,
        String utmContent
) {}
