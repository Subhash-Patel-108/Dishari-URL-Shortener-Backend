package com.dishari.in.application.serviceImpl;

import com.dishari.in.domain.entity.ShortUrl;
import com.dishari.in.domain.entity.User;

public class UrlSecurityService {

    public static boolean isOwner(User user , ShortUrl shortUrl) {
        return shortUrl.getUser().getId().equals(user.getId()) ;
    }
}
