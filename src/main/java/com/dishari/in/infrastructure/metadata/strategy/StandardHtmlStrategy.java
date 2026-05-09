package com.dishari.in.infrastructure.metadata.strategy;

import com.dishari.in.infrastructure.metadata.RawMetadata;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@Slf4j
public class StandardHtmlStrategy implements MetadataStrategy {

    @Override
    public String name() { return "StandardHtml"; }

    @Override
    public Optional<RawMetadata> extract(String url, String html) {
        try {
            Document doc = Jsoup.parse(html, url);

            // Title — multiple fallbacks
            String title = firstNonBlank(
                    doc.title(),
                    doc.select("h1").first() != null
                            ? doc.select("h1").first().text() : null
            );

            // Description
            String desc = firstNonBlank(
                    meta(doc, "description"),
                    meta(doc, "summary"),
                    meta(doc, "abstract")
            );

            // Author
            String author = firstNonBlank(
                    meta(doc, "author"),
                    meta(doc, "article:author")
            );

            // Favicon
            String favicon = extractFavicon(doc, url);

            // Site name from title tag pattern "Page Title | Site Name"
            String siteName = extractSiteName(title, url);

            if (title == null && desc == null) {
                return Optional.empty();
            }

            return Optional.of(new RawMetadata(
                    title, desc, null, favicon,
                    siteName, author, null, null
            ));

        } catch (Exception ex) {
            log.debug("StandardHtml strategy failed for {}: {}",
                    url, ex.getMessage());
            return Optional.empty();
        }
    }

    private String meta(Document doc, String name) {
        String val = doc.select("meta[name=" + name + "]")
                .attr("content");
        return val.isBlank() ? null : val;
    }

    private String extractFavicon(Document doc, String pageUrl) {
        // Try link tags in order of preference
        String[] selectors = {
                "link[rel='shortcut icon']",
                "link[rel='icon']",
                "link[rel='apple-touch-icon']",
                "link[rel='apple-touch-icon-precomposed']"
        };

        for (String selector : selectors) {
            String href = doc.select(selector).attr("href");
            if (!href.isBlank()) {
                return resolveUrl(href, pageUrl);
            }
        }

        // Final fallback — /favicon.ico
        return extractBaseUrl(pageUrl) + "/favicon.ico";
    }

    private String resolveUrl(String href, String pageUrl) {
        if (href.startsWith("http")) return href;
        if (href.startsWith("//")) return "https:" + href;
        if (href.startsWith("/")) return extractBaseUrl(pageUrl) + href;
        return extractBaseUrl(pageUrl) + "/" + href;
    }

    private String extractBaseUrl(String url) {
        try {
            java.net.URL u = new java.net.URL(url);
            return u.getProtocol() + "://" + u.getHost();
        } catch (Exception ex) {
            return "";
        }
    }

    private String extractSiteName(String title, String url) {
        // "Article Title | Site Name" pattern
        if (title != null && title.contains(" | ")) {
            String[] parts = title.split(" \\| ");
            return parts[parts.length - 1].trim();
        }
        if (title != null && title.contains(" - ")) {
            String[] parts = title.split(" - ");
            return parts[parts.length - 1].trim();
        }
        // Use domain as site name
        try {
            String host = new java.net.URL(url).getHost();
            return host.startsWith("www.")
                    ? host.substring(4) : host;
        } catch (Exception ex) {
            return null;
        }
    }

    private String firstNonBlank(String... values) {
        for (String val : values) {
            if (val != null && !val.isBlank()) return val;
        }
        return null;
    }
}