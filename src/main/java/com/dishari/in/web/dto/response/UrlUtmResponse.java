package com.dishari.in.web.dto.response;

public record UrlUtmResponse(
        String utmSource ,
        String utmMedium ,
        String utmCampaign ,
        String utmTerm ,
        String utmContent
) {

    public static UrlUtmResponse fromEntity(
            String utmSource ,
            String utmMedium ,
            String utmCampaign ,
            String utmTerm ,
            String utmContent
    ) {
        return new UrlUtmResponse(
                utmSource ,
                utmMedium ,
                utmCampaign ,
                utmTerm ,
                utmContent
        );
    }
}
