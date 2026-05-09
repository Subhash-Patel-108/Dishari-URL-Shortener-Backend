package com.dishari.in.infrastructure.metadata.strategy;

import com.dishari.in.infrastructure.metadata.RawMetadata;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class JsonLdStrategy implements MetadataStrategy {

    private final ObjectMapper objectMapper;

    @Override
    public String name() { return "JsonLD"; }

    @Override
    public Optional<RawMetadata> extract(String url, String html) {
        try {
            Document doc = Jsoup.parse(html, url);

            // Find all JSON-LD script blocks
            Elements scripts = doc.select(
                    "script[type=application/ld+json]");

            for (Element script : scripts) {
                try {
                    String json = script.data().trim();
                    if (json.isEmpty()) continue;

                    JsonNode node = objectMapper.readTree(json);

                    // Handle @graph array (common in WordPress sites)
                    if (node.has("@graph")) {
                        node = node.get("@graph");
                        if (node.isArray() && node.size() > 0) {
                            node = node.get(0);
                        }
                    }

                    String type   = getField(node, "@type");
                    String title  = getField(node,
                            "headline", "name");
                    String desc   = getField(node,
                            "description", "abstract");
                    String image  = extractImage(node);
                    String author = extractAuthor(node);
                    String site   = getField(node,
                            "publisher.name", "provider.name");

                    if (title != null || desc != null) {
                        return Optional.of(new RawMetadata(
                                title, desc, image, null,
                                site, author, null, type
                        ));
                    }

                } catch (Exception parseEx) {
                    log.debug("JSON-LD parse failed: {}",
                            parseEx.getMessage());
                }
            }

            return Optional.empty();

        } catch (Exception ex) {
            log.debug("JsonLD strategy failed for {}: {}",
                    url, ex.getMessage());
            return Optional.empty();
        }
    }

    private String getField(JsonNode node, String... paths) {
        for (String path : paths) {
            try {
                // Support dot notation — "publisher.name"
                String[] parts = path.split("\\.");
                JsonNode current = node;
                for (String part : parts) {
                    if (current == null || !current.has(part)) {
                        current = null;
                        break;
                    }
                    current = current.get(part);
                }
                if (current != null && current.isTextual()) {
                    String val = current.asText().trim();
                    if (!val.isEmpty()) return val;
                }
            } catch (Exception ex) {
                // continue to next path
            }
        }
        return null;
    }

    private String extractImage(JsonNode node) {
        if (!node.has("image")) return null;
        JsonNode img = node.get("image");

        if (img.isTextual()) return img.asText();
        if (img.isObject() && img.has("url")) {
            return img.get("url").asText();
        }
        if (img.isArray() && img.size() > 0) {
            JsonNode first = img.get(0);
            if (first.isTextual()) return first.asText();
            if (first.has("url")) return first.get("url").asText();
        }
        return null;
    }

    private String extractAuthor(JsonNode node) {
        if (!node.has("author")) return null;
        JsonNode author = node.get("author");

        if (author.isTextual()) return author.asText();
        if (author.isObject() && author.has("name")) {
            return author.get("name").asText();
        }
        return null;
    }
}