package com.dishari.in.web.dto.response;

import java.util.List;

public record BioPageDetailResponse(
        BioPageResponse bioPage,
        List<BioLinkResponse> links,
        Integer totalLinks,
        Integer activeLinksCount,
        Integer remainingSlots,
        Integer maxLinksAllowed
) {
    public boolean canAddMoreLinks() {
        return remainingSlots != null && remainingSlots > 0;
    }
}