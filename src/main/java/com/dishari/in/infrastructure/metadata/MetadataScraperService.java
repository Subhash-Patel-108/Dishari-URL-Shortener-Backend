package com.dishari.in.infrastructure.metadata;

import com.dishari.in.domain.entity.LinkMetadata;
import com.microsoft.playwright.*;
import com.microsoft.playwright.options.WaitUntilState;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;

import java.net.URL;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@Slf4j
public class MetadataScraperService {

    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/121.0.0.0 Safari/537.36";
    private static final Duration STATIC_TIMEOUT = Duration.ofSeconds(8);
    private static final Duration DYNAMIC_TIMEOUT = Duration.ofSeconds(12);
    private static final int MAX_RETRIES = 2;

    // Author extraction selectors
    private static final String[] AUTHOR_META_SELECTORS = {
            "meta[name='author']",
            "meta[property='article:author']",
            "meta[name='twitter:creator']",
            "meta[property='og:article:author']",
            "meta[name='dc.creator']",
            "meta[name='citation_author']",
            "meta[name='parsely-author']",
            "meta[name='sailthru.author']"
    };

    // Known domains with their destination types
    private static final Map<String, String> KNOWN_DOMAIN_TYPES = new HashMap<>();

    static {
        // Video platforms
        KNOWN_DOMAIN_TYPES.put("youtube.com", "video");
        KNOWN_DOMAIN_TYPES.put("youtu.be", "video");
        KNOWN_DOMAIN_TYPES.put("vimeo.com", "video");
        KNOWN_DOMAIN_TYPES.put("dailymotion.com", "video");
        KNOWN_DOMAIN_TYPES.put("twitch.tv", "video");
        KNOWN_DOMAIN_TYPES.put("netflix.com", "video");
        KNOWN_DOMAIN_TYPES.put("tiktok.com", "video");
        KNOWN_DOMAIN_TYPES.put("hotstar.com", "video");
        KNOWN_DOMAIN_TYPES.put("primevideo.com", "video");

        // E-commerce
        KNOWN_DOMAIN_TYPES.put("amazon.com", "ecommerce");
        KNOWN_DOMAIN_TYPES.put("amazon.in", "ecommerce");
        KNOWN_DOMAIN_TYPES.put("ebay.com", "ecommerce");
        KNOWN_DOMAIN_TYPES.put("flipkart.com", "ecommerce");
        KNOWN_DOMAIN_TYPES.put("aliexpress.com", "ecommerce");
        KNOWN_DOMAIN_TYPES.put("walmart.com", "ecommerce");
        KNOWN_DOMAIN_TYPES.put("shopify.com", "ecommerce");
        KNOWN_DOMAIN_TYPES.put("etsy.com", "ecommerce");
        KNOWN_DOMAIN_TYPES.put("myntra.com", "ecommerce");
        KNOWN_DOMAIN_TYPES.put("snapdeal.com", "ecommerce");

        // Social media
        KNOWN_DOMAIN_TYPES.put("twitter.com", "social_media");
        KNOWN_DOMAIN_TYPES.put("x.com", "social_media");
        KNOWN_DOMAIN_TYPES.put("facebook.com", "social_media");
        KNOWN_DOMAIN_TYPES.put("instagram.com", "social_media");
        KNOWN_DOMAIN_TYPES.put("linkedin.com", "social_media");
        KNOWN_DOMAIN_TYPES.put("reddit.com", "social_media");
        KNOWN_DOMAIN_TYPES.put("pinterest.com", "social_media");
        KNOWN_DOMAIN_TYPES.put("tumblr.com", "social_media");
        KNOWN_DOMAIN_TYPES.put("threads.net", "social_media");

        // Development
        KNOWN_DOMAIN_TYPES.put("github.com", "development");
        KNOWN_DOMAIN_TYPES.put("gitlab.com", "development");
        KNOWN_DOMAIN_TYPES.put("stackoverflow.com", "development");
        KNOWN_DOMAIN_TYPES.put("npmjs.com", "development");
        KNOWN_DOMAIN_TYPES.put("pypi.org", "development");

        // Blogging
        KNOWN_DOMAIN_TYPES.put("medium.com", "blog");
        KNOWN_DOMAIN_TYPES.put("wordpress.com", "blog");
        KNOWN_DOMAIN_TYPES.put("blogger.com", "blog");
        KNOWN_DOMAIN_TYPES.put("substack.com", "blog");
        KNOWN_DOMAIN_TYPES.put("hashnode.dev", "blog");
        KNOWN_DOMAIN_TYPES.put("dev.to", "blog");

        // News
        KNOWN_DOMAIN_TYPES.put("cnn.com", "news");
        KNOWN_DOMAIN_TYPES.put("bbc.com", "news");
        KNOWN_DOMAIN_TYPES.put("bbc.co.uk", "news");
        KNOWN_DOMAIN_TYPES.put("reuters.com", "news");
        KNOWN_DOMAIN_TYPES.put("nytimes.com", "news");
        KNOWN_DOMAIN_TYPES.put("theguardian.com", "news");
        KNOWN_DOMAIN_TYPES.put("wsj.com", "news");
        KNOWN_DOMAIN_TYPES.put("washingtonpost.com", "news");
        KNOWN_DOMAIN_TYPES.put("bloomberg.com", "news");
        KNOWN_DOMAIN_TYPES.put("timesofindia.indiatimes.com", "news");
        KNOWN_DOMAIN_TYPES.put("hindustantimes.com", "news");
        KNOWN_DOMAIN_TYPES.put("ndtv.com", "news");

        // Music
        KNOWN_DOMAIN_TYPES.put("spotify.com", "music");
        KNOWN_DOMAIN_TYPES.put("soundcloud.com", "music");
        KNOWN_DOMAIN_TYPES.put("apple.com", "music");

        // Education
        KNOWN_DOMAIN_TYPES.put("udemy.com", "education");
        KNOWN_DOMAIN_TYPES.put("coursera.org", "education");
        KNOWN_DOMAIN_TYPES.put("edx.org", "education");
        KNOWN_DOMAIN_TYPES.put("khanacademy.org", "education");

        // Encyclopedia
        KNOWN_DOMAIN_TYPES.put("wikipedia.org", "encyclopedia");
        KNOWN_DOMAIN_TYPES.put("wikimedia.org", "encyclopedia");
        KNOWN_DOMAIN_TYPES.put("britannica.com", "encyclopedia");
    }

    // ================================================================================
    // PUBLIC API
    // ================================================================================

    public Optional<LinkMetadata> scrape(String url) {
        log.debug("Starting metadata extraction for: {}", url);

        Optional<LinkMetadata> result = tryStaticExtraction(url);
        if (isCompleteAndValid(result)) {
            log.debug("Successfully extracted metadata via static extraction for: {}", url);
            return result;
        }

        result = tryDynamicExtraction(url);
        if (isCompleteAndValid(result)) {
            log.debug("Successfully extracted metadata via dynamic extraction for: {}", url);
            return result;
        }

        log.warn("Returning partial metadata for: {}", url);
        return result;
    }

    // ================================================================================
    // EXTRACTION STRATEGIES
    // ================================================================================

    private Optional<LinkMetadata> tryStaticExtraction(String url) {
        for (int attempt = 0; attempt < MAX_RETRIES; attempt++) {
            try {
                Connection.Response response = Jsoup.connect(url)
                        .userAgent(USER_AGENT)
                        .timeout((int) STATIC_TIMEOUT.toMillis())
                        .followRedirects(true)
                        .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                        .header("Accept-Language", "en-US,en;q=0.9")
                        .header("Accept-Encoding", "gzip, deflate, br")
                        .header("Cache-Control", "no-cache")
                        .header("Pragma", "no-cache")
                        .maxBodySize(5 * 1024 * 1024)
                        .execute();

                String responseContentType = response.contentType();
                if (responseContentType != null && responseContentType.contains("text/html")) {
                    Document doc = response.parse();
                    LinkMetadata metadata = buildMetadata(doc, url, responseContentType);

                    if (hasMinimumMetadata(metadata)) {
                        enrichMetadata(doc, metadata, url);
                        return Optional.of(metadata);
                    }
                }

                if (response.statusCode() == 200) break;

            } catch (java.net.SocketTimeoutException e) {
                log.warn("Static extraction timed out for {} (attempt {}/{})", url, attempt + 1, MAX_RETRIES);
                if (attempt < MAX_RETRIES - 1) sleep(1000L * (attempt + 1));
            } catch (org.jsoup.HttpStatusException e) {
                log.warn("HTTP {} for {} (attempt {}/{})", e.getStatusCode(), url, attempt + 1, MAX_RETRIES);
                if (e.getStatusCode() == 429 && attempt < MAX_RETRIES - 1) {
                    sleep(2000L * (attempt + 1));
                } else break;
            } catch (Exception e) {
                log.warn("Static extraction failed for {}: {}", url, e.getMessage());
                break;
            }
        }
        return Optional.empty();
    }

    private Optional<LinkMetadata> tryDynamicExtraction(String url) {
        try (Playwright playwright = Playwright.create()) {
            BrowserType.LaunchOptions launchOptions = new BrowserType.LaunchOptions()
                    .setHeadless(true)
                    .setTimeout(30000);

            try (Browser browser = playwright.chromium().launch(launchOptions)) {
                Browser.NewContextOptions contextOptions = new Browser.NewContextOptions()
                        .setUserAgent(USER_AGENT)
                        .setJavaScriptEnabled(true)
                        .setBypassCSP(true)
                        .setIgnoreHTTPSErrors(true);

                try (BrowserContext context = browser.newContext(contextOptions)) {
                    Page page = context.newPage();
                    page.setDefaultTimeout(DYNAMIC_TIMEOUT.toMillis());

                    try {
                        page.navigate(url, new Page.NavigateOptions()
                                .setTimeout(DYNAMIC_TIMEOUT.toMillis())
                                .setWaitUntil(WaitUntilState.DOMCONTENTLOADED));

                        try {
                            page.waitForSelector("meta[property='og:title'], title",
                                    new Page.WaitForSelectorOptions().setTimeout(3000));
                        } catch (Exception e) {
                            log.debug("Metadata elements not found within 3s for: {}", url);
                        }

                        String html = page.content();
                        Document doc = Jsoup.parse(html);
                        LinkMetadata metadata = buildMetadata(doc, url, "text/html");
                        enrichMetadata(doc, metadata, url);
                        return Optional.of(metadata);

                    } catch (TimeoutError e) {
                        log.warn("Playwright navigation timeout for {}: {}", url, e.getMessage());
                        try {
                            String html = page.content();
                            if (html != null && !html.isEmpty()) {
                                Document doc = Jsoup.parse(html);
                                return Optional.of(buildMetadata(doc, url, "text/html"));
                            }
                        } catch (Exception ex) {
                            log.error("Failed to get partial content for {}: {}", url, ex.getMessage());
                        }
                        return Optional.empty();
                    } finally {
                        page.close();
                    }
                }
            }
        } catch (Exception e) {
            log.error("Dynamic extraction failed for {}: {}", url, e.getMessage());
            return Optional.empty();
        }
    }

    // ================================================================================
    // METADATA BUILDING
    // ================================================================================

    private LinkMetadata buildMetadata(Document doc, String url, String responseContentType) {
        return LinkMetadata.builder()
                .title(truncate(extractTitle(doc), 255))
                .description(truncate(extractDescription(doc), 500))
                .imageUrl(extractImage(doc))
                .faviconUrl(extractFavicon(doc, url))
                .siteName(extractSiteName(doc, url))
                .author(extractAuthor(doc, url))
                .canonicalUrl(extractCanonicalUrl(doc, url))
                .destinationType(determineDestinationType(doc, url))
                .contentType(determineContentType(responseContentType, url))
                .lastScrapedAt(Instant.now())
                .build();
    }

    // ================================================================================
    // BASIC METADATA EXTRACTION
    // ================================================================================

    private String extractTitle(Document doc) {
        String title = getMetaContent(doc,
                "meta[property='og:title']",
                "meta[name='twitter:title']",
                "meta[name='title']",
                "meta[property='title']"
        );

        if (title != null && !title.isBlank()) return title;

        title = doc.title();
        if (title != null && !title.isBlank()) return title;

        return doc.select("h1").text();
    }

    private String extractDescription(Document doc) {
        String description = getMetaContent(doc,
                "meta[name='description']",
                "meta[property='og:description']",
                "meta[name='twitter:description']",
                "meta[itemprop='description']"
        );

        if (description != null && description.length() > 10) return description;

        return doc.select("p").stream()
                .map(p -> p.text().trim())
                .filter(t -> t.length() > 30)
                .findFirst()
                .orElse("No preview available");
    }

    private String extractImage(Document doc) {
        String image = getMetaContent(doc,
                "meta[property='og:image']",
                "meta[name='twitter:image']",
                "meta[name='twitter:image:src']",
                "meta[itemprop='image']"
        );

        if (image != null && !image.isEmpty()) return image;

        return doc.select("img[src]").stream()
                .filter(img -> {
                    String src = img.absUrl("src");
                    return src != null && !src.isEmpty() &&
                            !src.contains("data:image") &&
                            !src.contains("spacer") &&
                            !src.contains("pixel");
                })
                .map(img -> img.absUrl("src"))
                .findFirst()
                .orElse(null);
    }

    private String extractFavicon(Document doc, String url) {
        String[] selectors = {
                "link[rel='icon'][type='image/png']",
                "link[rel='icon'][type='image/x-icon']",
                "link[rel='shortcut icon']",
                "link[rel='icon']",
                "link[rel='apple-touch-icon']",
                "link[rel='apple-touch-icon-precomposed']"
        };

        for (String selector : selectors) {
            String href = doc.select(selector).attr("href");
            if (!href.isEmpty()) return makeAbsoluteUrl(href, url);
        }

        try {
            URL urlObj = new URL(url);
            return urlObj.getProtocol() + "://" + urlObj.getHost() + "/favicon.ico";
        } catch (Exception e) {
            return null;
        }
    }

    private String extractSiteName(Document doc, String url) {
        String siteName = getMetaContent(doc,
                "meta[property='og:site_name']",
                "meta[name='application-name']",
                "meta[name='twitter:site']"
        );

        if (siteName != null && !siteName.isBlank()) return siteName;

        try {
            String host = new URL(url).getHost().replace("www.", "");
            return host.substring(0, 1).toUpperCase() + host.substring(1);
        } catch (Exception e) {
            return null;
        }
    }

    // ================================================================================
    // AUTHOR EXTRACTION
    // ================================================================================

    private String extractAuthor(Document doc, String url) {
        // 1. Meta tags
        for (String selector : AUTHOR_META_SELECTORS) {
            String author = getElementContent(doc, selector);
            if (isValidAuthor(author)) return cleanAuthor(author);
        }

        // 2. JSON-LD
        String jsonLdAuthor = extractAuthorFromJsonLd(doc);
        if (isValidAuthor(jsonLdAuthor)) return cleanAuthor(jsonLdAuthor);

        // 3. Class patterns
        String classAuthor = extractAuthorFromClassPatterns(doc);
        if (isValidAuthor(classAuthor)) return cleanAuthor(classAuthor);

        // 4. Schema markup
        String schemaAuthor = extractAuthorFromSchemaMarkup(doc);
        if (isValidAuthor(schemaAuthor)) return cleanAuthor(schemaAuthor);

        // 5. URL patterns
        String urlAuthor = extractAuthorFromUrl(url);
        if (isValidAuthor(urlAuthor)) return cleanAuthor(urlAuthor);

        // 6. rel=author
        String relAuthor = doc.select("a[rel='author']").text();
        if (isValidAuthor(relAuthor)) return cleanAuthor(relAuthor);

        return null;
    }

    private String extractAuthorFromJsonLd(Document doc) {
        for (Element script : doc.select("script[type='application/ld+json']")) {
            try {
                String json = script.html();
                String[] patterns = {
                        "\"author\"\\s*:\\s*\\{[^}]*\"name\"\\s*:\\s*\"([^\"]+)\"",
                        "\"author\"\\s*:\\s*\"([^\"]+)\"",
                        "\"creator\"\\s*:\\s*\"([^\"]+)\"",
                        "\"author\"\\s*:\\s*\\[\\s*\\{[^}]*\"name\"\\s*:\\s*\"([^\"]+)\""
                };

                for (String pattern : patterns) {
                    Matcher matcher = Pattern.compile(pattern).matcher(json);
                    if (matcher.find()) return matcher.group(1);
                }
            } catch (Exception ignored) {}
        }
        return null;
    }

    private String extractAuthorFromClassPatterns(Document doc) {
        String[] patterns = {
                "[class*='author-name']", "[class*='byline']",
                "[class*='post-author']", "[class*='entry-author']",
                "[rel='author']", "[itemprop='author'] [itemprop='name']",
                ".author .name", ".byline .author"
        };

        for (String pattern : patterns) {
            Element element = doc.selectFirst(pattern);
            if (element != null) {
                String text = element.text().trim();
                if (isValidAuthor(text)) return text;
            }
        }
        return null;
    }

    private String extractAuthorFromSchemaMarkup(Document doc) {
        String[] selectors = {
                "[itemtype*='schema.org/Person'] [itemprop='name']",
                "[itemtype*='schema.org/Organization'] [itemprop='name']",
                "[itemprop='author'] [itemprop='name']",
                "[itemprop='creator'] [itemprop='name']"
        };

        for (String selector : selectors) {
            Element element = doc.selectFirst(selector);
            if (element != null) {
                String name = element.text().trim();
                if (isValidAuthor(name)) return name;
            }
        }
        return null;
    }

    private String extractAuthorFromUrl(String url) {
        try {
            String path = new URL(url).getPath();

            Matcher matcher = Pattern.compile("/@([^/]+)").matcher(path);
            if (matcher.find()) return matcher.group(1);

            matcher = Pattern.compile("/(?:author|authors)/([^/]+)").matcher(path);
            if (matcher.find()) return matcher.group(1).replace("-", " ").replace("_", " ");
        } catch (Exception ignored) {}
        return null;
    }

    private boolean isValidAuthor(String author) {
        if (author == null || author.isEmpty()) return false;
        String cleaned = author.trim();
        return cleaned.length() >= 2 && cleaned.length() <= 100 &&
                !cleaned.equalsIgnoreCase("admin") &&
                !cleaned.equalsIgnoreCase("author") &&
                !cleaned.equalsIgnoreCase("unknown") &&
                !cleaned.equalsIgnoreCase("staff") &&
                !cleaned.matches("\\d+") &&
                !cleaned.matches("^[\\w.-]+@[\\w.-]+\\.\\w{2,}$");
    }

    private String cleanAuthor(String author) {
        if (author == null) return null;
        return author.replaceAll("\\s+", " ")
                .replaceAll("(?i)^\\s*(by|author:|written by|posted by)\\s+", "")
                .trim();
    }

    // ================================================================================
    // CANONICAL URL EXTRACTION
    // ================================================================================

    private String extractCanonicalUrl(Document doc, String currentUrl) {
        String canonical = doc.select("link[rel='canonical']").attr("href");
        if (!canonical.isEmpty()) return makeAbsoluteUrl(canonical, currentUrl);

        String ogUrl = doc.select("meta[property='og:url']").attr("content");
        if (!ogUrl.isEmpty()) return ogUrl;

        String twitterUrl = doc.select("meta[name='twitter:url']").attr("content");
        if (!twitterUrl.isEmpty()) return twitterUrl;

        return currentUrl;
    }

    // ================================================================================
    // DESTINATION TYPE DETECTION
    // ================================================================================

    private String determineDestinationType(Document doc, String url) {
        // 1. Known domain check
        String domainType = checkKnownDomain(url);
        if (domainType != null) return domainType;

        // 2. Open Graph type
        String ogType = doc.select("meta[property='og:type']").attr("content");
        if (!ogType.isEmpty()) {
            String mappedType = mapOgToDestinationType(ogType);
            if (mappedType != null) return mappedType;
        }

        // 3. Schema.org type
        String schemaType = extractSchemaDestinationType(doc);
        if (schemaType != null) return schemaType;

        // 4. Twitter card
        String twitterCard = doc.select("meta[name='twitter:card']").attr("content");
        if (twitterCard.equalsIgnoreCase("player")) return "video";

        // 5. Page content analysis
        if (hasVideoIndicators(doc)) return "video";
        if (hasEcommerceIndicators(doc)) return "ecommerce";
        if (hasNewsIndicators(doc)) return "news";
        if (hasBlogIndicators(doc)) return "blog";

        // 6. URL pattern analysis
        String urlType = inferTypeFromUrl(url);
        if (urlType != null) return urlType;

        // 7. Article check
        if (doc.select("meta[property='og:type']").attr("content").equalsIgnoreCase("article")) {
            return "article";
        }

        // 8. Default
        return "website";
    }

    private String checkKnownDomain(String url) {
        try {
            String host = new URL(url).getHost().toLowerCase().replace("www.", "");

            // Direct match
            if (KNOWN_DOMAIN_TYPES.containsKey(host)) return KNOWN_DOMAIN_TYPES.get(host);

            // Parent domain match (sub.example.com -> example.com)
            String[] parts = host.split("\\.");
            if (parts.length > 2) {
                String parentDomain = parts[parts.length - 2] + "." + parts[parts.length - 1];
                if (KNOWN_DOMAIN_TYPES.containsKey(parentDomain)) {
                    return KNOWN_DOMAIN_TYPES.get(parentDomain);
                }
            }
        } catch (Exception ignored) {}
        return null;
    }

    private String mapOgToDestinationType(String ogType) {
        switch (ogType.toLowerCase()) {
            case "video.movie":
            case "video.episode":
            case "video.tv_show":
            case "video.other":
            case "video":
                return "video";
            case "article":
                return "article";
            case "product":
            case "product.item":
                return "ecommerce";
            case "profile":
                return "social_media";
            case "music.song":
            case "music.album":
            case "music.playlist":
            case "music.radio_station":
                return "music";
            case "book":
                return "education";
            default:
                return null;
        }
    }

    private String extractSchemaDestinationType(Document doc) {
        // Check itemtype attribute
        Element schemaElement = doc.selectFirst("[itemtype*='schema.org/']");
        if (schemaElement != null) {
            String itemtype = schemaElement.attr("itemtype").toLowerCase();
            if (itemtype.contains("product")) return "ecommerce";
            if (itemtype.contains("newsarticle")) return "news";
            if (itemtype.contains("article")) return "article";
            if (itemtype.contains("blogposting") || itemtype.contains("blog")) return "blog";
            if (itemtype.contains("videoobject")) return "video";
            if (itemtype.contains("music")) return "music";
            if (itemtype.contains("person") || itemtype.contains("profile")) return "social_media";
            if (itemtype.contains("course") || itemtype.contains("book")) return "education";
        }

        // Check JSON-LD
        for (Element script : doc.select("script[type='application/ld+json']")) {
            try {
                String json = script.html();
                Matcher matcher = Pattern.compile("\"@type\"\\s*:\\s*\"([^\"]+)\"",
                        Pattern.CASE_INSENSITIVE).matcher(json);
                if (matcher.find()) {
                    String type = matcher.group(1).toLowerCase();
                    if (type.contains("product")) return "ecommerce";
                    if (type.contains("news")) return "news";
                    if (type.contains("article")) return "article";
                    if (type.contains("blog")) return "blog";
                    if (type.contains("video")) return "video";
                    if (type.contains("music")) return "music";
                    if (type.contains("person") || type.contains("profile")) return "social_media";
                    if (type.contains("course") || type.contains("education")) return "education";
                    if (type.contains("software") || type.contains("code")) return "development";
                }
            } catch (Exception ignored) {}
        }
        return null;
    }

    private boolean hasVideoIndicators(Document doc) {
        if (!doc.select("video").isEmpty()) return true;
        if (!doc.select("[class*='video-player'], [class*='videoPlayer'], [id*='video-player']").isEmpty()) return true;
        if (!doc.select("iframe[src*='youtube.com'], iframe[src*='vimeo.com'], iframe[src*='dailymotion.com']").isEmpty()) return true;
        if (!doc.select("meta[property='og:video']").attr("content").isEmpty()) return true;
        if (!doc.select("[itemtype*='VideoObject']").isEmpty()) return true;
        return false;
    }

    private boolean hasEcommerceIndicators(Document doc) {
        if (!doc.select("[class*='product'], [id*='product']").isEmpty()) return true;
        if (!doc.select("[class*='price'], [id*='price']").isEmpty()) return true;
        if (!doc.select("[class*='add-to-cart'], [aria-label*='cart'], [class*='shopify'], [class*='woocommerce']").isEmpty()) return true;
        if (!doc.select("button:contains(Add to Cart), button:contains(Buy Now), a:contains(Add to Cart)").isEmpty()) return true;
        if (!doc.select("[itemtype*='Product']").isEmpty()) return true;
        if (!doc.select("meta[property='product:price']").attr("content").isEmpty()) return true;
        return false;
    }

    private boolean hasNewsIndicators(Document doc) {
        if (!doc.select("[itemtype*='NewsArticle'], [itemtype*='Article']").isEmpty()) return true;
        if (!doc.select("[class*='article'], [class*='news'], [class*='headline'], [class*='breaking']").isEmpty()) return true;
        if (!doc.select("[class*='byline']").isEmpty()) return true;
        if (!doc.select("meta[name='news_keywords']").attr("content").isEmpty()) return true;
        if (!doc.select("meta[property='article:published_time']").attr("content").isEmpty()) return true;
        return false;
    }

    private boolean hasBlogIndicators(Document doc) {
        if (!doc.select("[itemtype*='BlogPosting'], [itemtype*='Blog']").isEmpty()) return true;
        if (!doc.select("[class*='blog-post'], [class*='blogpost'], [class*='post-'], [class*='entry-title'], [class*='entry-content']").isEmpty()) return true;
        return false;
    }

    private String inferTypeFromUrl(String url) {
        String lowerUrl = url.toLowerCase();

        if (lowerUrl.matches(".*/(video|watch|videos|embed|clip|movie)/.*")) return "video";
        if (lowerUrl.matches(".*/(news|article|story|breaking|headlines)/.*")) return "news";
        if (lowerUrl.matches(".*/(blog|post|posts|articles|entry)/.*")) return "blog";
        if (lowerUrl.matches(".*/(product|item|shop|store|buy|cart|checkout|collection)/.*")) return "ecommerce";
        if (lowerUrl.matches(".*/(profile|user|users|status|feed)/.*")) return "social_media";
        if (lowerUrl.matches(".*/(docs|documentation|api|guide|tutorial)/.*")) return "documentation";
        if (lowerUrl.matches(".*/(forum|topic|thread|discussion|community)/.*")) return "forum";

        if (lowerUrl.matches(".*\\.(mp4|webm|avi|mov|mkv|flv)$")) return "video";
        if (lowerUrl.matches(".*\\.(mp3|wav|ogg|flac|aac|m4a)$")) return "music";
        if (lowerUrl.matches(".*\\.(pdf|doc|docx|ppt|pptx|xls|xlsx)$")) return "document";

        return null;
    }

    // ================================================================================
    // CONTENT TYPE DETECTION (MIME)
    // ================================================================================

    private String determineContentType(String headerContentType, String url) {
        if (headerContentType != null) {
            return headerContentType.split(";")[0].trim();
        }

        String lowerUrl = url.toLowerCase();
        if (lowerUrl.matches(".*\\.(jpg|jpeg|png|gif|webp|svg|bmp|ico)$")) return "image/*";
        if (lowerUrl.matches(".*\\.(mp4|webm|avi|mov|mkv|flv|wmv)$")) return "video/*";
        if (lowerUrl.matches(".*\\.(mp3|wav|ogg|flac|aac|m4a|wma)$")) return "audio/*";
        if (lowerUrl.matches(".*\\.(pdf)$")) return "application/pdf";
        if (lowerUrl.matches(".*\\.(html?|php|asp|jsp|do)$")) return "text/html";
        if (lowerUrl.matches(".*\\.(json)$")) return "application/json";
        if (lowerUrl.matches(".*\\.(xml)$")) return "application/xml";
        if (lowerUrl.matches(".*\\.(css)$")) return "text/css";
        if (lowerUrl.matches(".*\\.(js)$")) return "application/javascript";

        return "text/html";
    }

    // ================================================================================
    // METADATA ENRICHMENT
    // ================================================================================

    private void enrichMetadata(Document doc, LinkMetadata metadata, String url) {
        for (Element script : doc.select("script[type='application/ld+json']")) {
            try {
                String json = script.html();

                if (isBlank(metadata.getTitle())) {
                    String name = extractJsonValue(json, "name");
                    if (name == null) name = extractJsonValue(json, "headline");
                    if (name != null) metadata.setTitle(name);
                }

                if (isBlank(metadata.getDescription())) {
                    String desc = extractJsonValue(json, "description");
                    if (desc != null) metadata.setDescription(desc);
                }

                if (metadata.getImageUrl() == null) {
                    String img = extractJsonValue(json, "image");
                    if (img == null) img = extractJsonValue(json, "thumbnailUrl");
                    if (img != null) metadata.setImageUrl(img);
                }

                if (metadata.getAuthor() == null) {
                    String author = extractJsonValue(json, "author");
                    if (author != null && isValidAuthor(author)) {
                        metadata.setAuthor(cleanAuthor(author));
                    }
                }
            } catch (Exception ignored) {}
        }
    }

    // ================================================================================
    // UTILITY METHODS
    // ================================================================================

    private String getMetaContent(Document doc, String... selectors) {
        for (String selector : selectors) {
            String content = doc.select(selector).attr("content").trim();
            if (!content.isEmpty()) return content;
        }
        return null;
    }

    private String getElementContent(Document doc, String selector) {
        Element element = doc.selectFirst(selector);
        if (element != null) {
            String content = element.attr("content").trim();
            if (!content.isEmpty()) return content;
            return element.text().trim();
        }
        return null;
    }

    private String extractJsonValue(String json, String key) {
        Matcher matcher = Pattern.compile("\"" + key + "\"\\s*:\\s*\"([^\"]+)\"").matcher(json);
        if (matcher.find()) return matcher.group(1);

        matcher = Pattern.compile("\"" + key + "\"\\s*:\\s*\\{[^}]*\"url\"\\s*:\\s*\"([^\"]+)\"").matcher(json);
        if (matcher.find()) return matcher.group(1);

        return null;
    }

    private String makeAbsoluteUrl(String href, String baseUrl) {
        if (href == null || href.isEmpty()) return null;
        if (href.startsWith("http://") || href.startsWith("https://")) return href;
        try {
            return new URL(new URL(baseUrl), href).toString();
        } catch (Exception e) {
            return null;
        }
    }

    private boolean hasMinimumMetadata(LinkMetadata metadata) {
        return metadata.getTitle() != null &&
                !metadata.getTitle().equals("Link Preview Unavailable");
    }

    private boolean isCompleteAndValid(Optional<LinkMetadata> result) {
        return result.isPresent() &&
                result.get().getTitle() != null &&
                !result.get().getTitle().equals("Link Preview Unavailable");
    }

    private boolean isBlank(String str) {
        return str == null || str.isBlank();
    }

    private String truncate(String text, int maxLength) {
        if (text == null) return "Link Preview Unavailable";
        String fallback = text.equals("Link Preview Unavailable") ? text :
                text.equals("No preview available") ? text :
                "Link Preview Unavailable";
        if (text.length() <= maxLength) return text;
        return text.substring(0, maxLength - 3) + "...";
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}