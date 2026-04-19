package com.dishari.in.utils;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.util.StringUtils;

public class IPUtils {

    private static final String[] IP_HEADER_CANDIDATES = {
            "X-Forwarded-For",
            "Proxy-Client-IP",
            "WL-Proxy-Client-IP",
            "HTTP_X_FORWARDED_FOR",
            "HTTP_X_FORWARDED",
            "HTTP_X_CLUSTER_CLIENT_IP",
            "HTTP_CLIENT_IP",
            "HTTP_FORWARDED_FOR",
            "HTTP_FORWARDED",
            "HTTP_VIA",
            "REMOTE_ADDR"
    };

    //Method to extract IP from HttpServletRequest
    public static String extractIpAddress(HttpServletRequest request) {
        for(String header : IP_HEADER_CANDIDATES) {
            String ipAddress = request.getHeader(header) ;

            //Now we check that the ip is correct or not
            if (StringUtils.hasText(ipAddress) && !"unknown".equalsIgnoreCase(ipAddress)) {
                // The first one is the original client.
                return ipAddress.split(",")[0].trim() ;
            }
        }
        return request.getRemoteAddr() ;
    }

    //Method to extract UserAgent from HttpServletRequest
    public static String extractUserAgent(HttpServletRequest request) {
        return request.getHeader("User-Agent");
    }
}
