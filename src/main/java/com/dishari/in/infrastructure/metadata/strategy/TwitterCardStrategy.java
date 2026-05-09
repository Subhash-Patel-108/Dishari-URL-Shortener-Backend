package com.dishari.in.infrastructure.metadata.strategy;

import com.dishari.in.infrastructure.metadata.RawMetadata;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@Slf4j
public class TwitterCardStrategy implements MetadataStrategy {

    @Override
    public String name() { return "TwitterCard"; }

    @Override
    public Optional<RawMetadata> extract(String url, String html) {
        try {
            Document doc = Jsoup.parse(html, url);

            String title = meta(doc, "twitter:title");
            String desc  = meta(doc, "twitter:description");
            String image = meta(doc, "twitter:image");
            String site  = meta(doc, "twitter:site"); // @handle

            if (title == null && desc == null && image == null) {
                return Optional.empty();
            }

            return Optional.of(new RawMetadata(
                    title, desc, image, null,
                    site, null, null, null
            ));

        } catch (Exception ex) {
            log.debug("TwitterCard strategy failed for {}: {}",
                    url, ex.getMessage());
            return Optional.empty();
        }
    }

    private String meta(Document doc, String name) {
        String val = doc.select("meta[name=" + name + "]")
                .attr("content");
        if (!val.isBlank()) return val;

        val = doc.select("meta[property=" + name + "]")
                .attr("content");
        return val.isBlank() ? null : val;
    }
}