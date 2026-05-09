package com.dishari.in.infrastructure.metadata;

import lombok.extern.slf4j.Slf4j;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@Slf4j
public class MetadataHttpFetcher {

    private static final int TIMEOUT_MS   = 8000;
    private static final int MAX_BODY_SIZE = 1024 * 1024; // 1MB — enough for meta tags

    // Rotate user agents — reduces bot detection
    private static final String[] USER_AGENTS = {
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) " +
                    "AppleWebKit/537.36 (KHTML, like Gecko) " +
                    "Chrome/122.0.0.0 Safari/537.36",
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) " +
                    "AppleWebKit/537.36 (KHTML, like Gecko) " +
                    "Chrome/122.0.0.0 Safari/537.36",
            "Mozilla/5.0 (iPhone; CPU iPhone OS 17_0 like Mac OS X) " +
                    "AppleWebKit/605.1.15 (KHTML, like Gecko) " +
                    "Version/17.0 Mobile/15E148 Safari/604.1"
    };

    public Optional<String> fetchHtml(String url) {
        // Try each user agent — some sites block specific ones
        for (String userAgent : USER_AGENTS) {
            Optional<String> html = tryFetch(url, userAgent);
            if (html.isPresent()) return html;
        }
        return Optional.empty();
    }

    private Optional<String> tryFetch(String url, String userAgent) {
        try {
            Connection.Response response = Jsoup.connect(url)
                    .userAgent(userAgent)
                    .timeout(TIMEOUT_MS)
                    .maxBodySize(MAX_BODY_SIZE)
                    .followRedirects(true)
                    // Realistic browser headers
                    .header("Accept",
                            "text/html,application/xhtml+xml," +
                                    "application/xml;q=0.9,*/*;q=0.8")
                    .header("Accept-Language", "en-US,en;q=0.9")
                    .header("Accept-Encoding", "gzip, deflate, br")
                    .header("Connection", "keep-alive")
                    .header("Upgrade-Insecure-Requests", "1")
                    .header("Sec-Fetch-Dest", "document")
                    .header("Sec-Fetch-Mode", "navigate")
                    .header("Sec-Fetch-Site", "none")
                    .ignoreHttpErrors(true) // don't throw on 4xx/5xx
                    .execute();

            int statusCode = response.statusCode();

            // Accept 200 and 301/302 final destinations
            if (statusCode != 200) {
                log.debug("Non-200 status {} for url={}", statusCode, url);
                return Optional.empty();
            }

            // Only parse HTML content
            String contentType = response.contentType();
            if (contentType == null ||
                    !contentType.contains("text/html")) {
                log.debug("Non-HTML content type: {} for url={}",
                        contentType, url);
                return Optional.empty();
            }

            return Optional.of(response.body());

        } catch (Exception ex) {
            log.debug("Fetch failed with userAgent={}: {}",
                    userAgent.substring(0, 20), ex.getMessage());
            return Optional.empty();
        }
    }
}