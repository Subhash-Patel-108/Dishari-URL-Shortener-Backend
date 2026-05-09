package com.dishari.in.utils;

import com.google.common.net.InternetDomainName;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.URISyntaxException;

@Component
@Slf4j
public class DomainUtil {
    public static String extractRootDomain(String url) {
        if (url == null || url.isBlank()) return null;

        try {
            // 1. Extract the host using URI (faster and safer than regex)
            URI uri = new URI(url);
            String host = uri.getHost();

            if (host == null) return null;

            // 2. Use Guava to parse the domain structure
            InternetDomainName domainName = InternetDomainName.from(host);

            // 3. Check if it's a valid public domain (e.g., not "localhost" or an IP)
            if (domainName.isUnderPublicSuffix()) {
                return domainName.topPrivateDomain().toString();
            }

            return host; // Fallback for IPs or internal hostnames
        } catch (Exception e) {
            log.error("Failed to extract root domain from URL: {}", url);
            return null;
        }
    }
}
