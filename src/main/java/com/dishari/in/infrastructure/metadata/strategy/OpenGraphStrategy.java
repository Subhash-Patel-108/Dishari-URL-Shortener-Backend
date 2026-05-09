package com.dishari.in.infrastructure.metadata.strategy;

import com.dishari.in.infrastructure.metadata.RawMetadata;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@Slf4j
public class OpenGraphStrategy implements MetadataStrategy {

    @Override
    public String name() { return "OpenGraph"; }

    @Override
    public Optional<RawMetadata> extract(String url, String html) {
        try {
            Document doc = Jsoup.parse(html, url);

            String title    = og(doc, "og:title");
            String desc     = og(doc, "og:description");
            String image    = og(doc, "og:image");
            String siteName = og(doc, "og:site_name");
            String type     = og(doc, "og:type");
            String canonical = og(doc, "og:url");

            // Resolve relative image URLs to absolute
            if (image != null && image.startsWith("/")) {
                image = extractBaseUrl(url) + image;
            }

            if (title == null && desc == null && image == null) {
                return Optional.empty();
            }

            return Optional.of(new RawMetadata(
                    title, desc, image, null, siteName,
                    null, canonical, type
            ));

        } catch (Exception ex) {
            log.debug("OpenGraph strategy failed for {}: {}",
                    url, ex.getMessage());
            return Optional.empty();
        }
    }

    private String og(Document doc, String property) {
        // Try property attribute (standard OG)
        String val = doc.select("meta[property=" + property + "]")
                .attr("content");
        if (!val.isBlank()) return val;

        // Try name attribute (some sites use this)
        val = doc.select("meta[name=" + property + "]")
                .attr("content");
        return val.isBlank() ? null : val;
    }

    private String extractBaseUrl(String url) {
        try {
            java.net.URL u = new java.net.URL(url);
            return u.getProtocol() + "://" + u.getHost();
        } catch (Exception ex) {
            return "";
        }
    }
}