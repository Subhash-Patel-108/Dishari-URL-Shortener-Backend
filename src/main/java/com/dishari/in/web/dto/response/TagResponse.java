package com.dishari.in.web.dto.response;

import com.dishari.in.domain.entity.Tag;

public record TagResponse(
        String name ,
        String color
) {
    public static TagResponse fromEntity(Tag tag) {
        return new TagResponse(tag.getName() , tag.getColor()) ;
    }
}
