package com.dishari.in.application.serviceImpl;

import com.dishari.in.application.service.RedirectService;
import com.dishari.in.domain.entity.LinkMetadata;
import com.dishari.in.domain.entity.ShortUrl;
import com.dishari.in.domain.repository.LinkMetadataRepository;
import com.dishari.in.domain.repository.ShortUrlRepository;
import com.dishari.in.exception.UrlNotFoundException;
import com.dishari.in.infrastructure.cache.SlugRedirectCacheService;
import com.dishari.in.infrastructure.messaging.producer.LinkMetadataEventProducer;
import com.dishari.in.infrastructure.metadata.MetadataScraperService;
import com.dishari.in.infrastructure.redirect.RedirectData;
import com.dishari.in.infrastructure.redirect.RuleEngine;
import com.dishari.in.utils.DomainUtil;
import com.dishari.in.web.dto.request.UnlockSlugRequest;
import com.dishari.in.web.dto.response.RedirectPreviewResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class RedirectServiceImpl implements RedirectService {
    private final LinkMetadataRepository linkMetadataRepository;
    private final LinkMetadataEventProducer linkMetadataEventProducer ;
    private final ShortUrlRepository shortUrlRepository;
    private final SlugRedirectCacheService slugRedirectCacheService ;
    private final RuleEngine ruleEngine ;
    private final PasswordEncoder passwordEncoder ;
    private final MetadataScraperService metadataScraperService ;

    @Value("${app.frontend-base-url}")
    private String frontendBaseUrl;

    @Override
    @Transactional(readOnly = true)
    public RedirectData redirect(String slug , HttpServletRequest servletRequest , boolean isPasswordChecked) {
        // 1. Try to get everything from Cache
        return slugRedirectCacheService.getAndIncrement(slug)
                .map(result -> {
                    log.info("Cache hit for slug : {}" , slug);
                    //Format : id|originalUrl|flagged|maxClicks|expiresAt|isPasswordChecked
                    String[] parts = result.metadata().split("\\|");
                    String shortUrlId = parts[0] ;
                    long currentClicks = result.clickCount();
                    long maxClicks = Long.parseLong(parts[3]);
                    log.info("Click Count = {} :: redirect() :: slug = {}" , currentClicks , slug );
                    // 2. Click Limit Check
                    if (maxClicks > 0 && currentClicks > maxClicks) {
                        log.warn("Slug {} reached max clicks ({})", slug, maxClicks);
                        return new RedirectData(parts[0], "/max-clicks-reached", 403 , "MAX_LIMIT"); // Forbidden
                    }

                    // 3. Safety Check
                    if (Boolean.parseBoolean(parts[2]) && !isPasswordChecked) {
                        return RedirectData.unsafe(shortUrlId , slug , frontendBaseUrl);
                    }

                    // parts[4] is the expiresAt string
                    String expiryStr = parts[4];
                    if (!expiryStr.equals("null")) {
                        Instant expiresAt = Instant.parse(expiryStr);
                        if (expiresAt.isBefore(Instant.now())) {
                            log.info("Slug {} has expired at {}", slug, expiresAt);
                            return RedirectData.expired(shortUrlId , slug , frontendBaseUrl); // 410 Gone
                        }
                    }

                    //check for is password checked part[5] --> is Password Checked
                    if (Boolean.parseBoolean(parts[5]) && !isPasswordChecked ) {
                        return RedirectData.passwordProtected(parts[0],slug , frontendBaseUrl) ;
                    }

                    // 4. Return success redirect
                    return RedirectData.simple(shortUrlId , parts[1]);
                })
                .orElseGet(() -> {
                    // 5. Cache Miss -> Fallback to DB, then put in cache
                    log.info("Cache miss for slug : {}" , slug);
                    return handleCacheMiss(slug , servletRequest , isPasswordChecked);
                });
    }

    @Override
    @Transactional(readOnly = true)
    public RedirectData unlock(String slug , UnlockSlugRequest slugRequest, HttpServletRequest request) {
        //Now find the shortUrl using slug
        Optional<ShortUrl> shortUrlOptional = shortUrlRepository.findBySlugAndDeletedAtIsNull(slug) ;

        if (shortUrlOptional.isEmpty()) {
            return RedirectData.notFound(slug , frontendBaseUrl) ;
        }

        ShortUrl shortUrl = shortUrlOptional.get() ;

        //We check that the password is currect or not
        if (!passwordEncoder.matches(slugRequest.password() , shortUrl.getHashedPassword())) {
            //TODO: Applying rate limit by ip on password try
            log.info("Password not matches :: unlock() :: slug = {}" , slug );
            return RedirectData.passwordProtected(shortUrl.getId().toString() , slug , frontendBaseUrl) ;
        }

        //if the password is correct then redirect user
        return redirect(slug , request , true) ;
    }

    @Override
    @Transactional(readOnly = true)
    public RedirectPreviewResponse redirectPreview(String slug, HttpServletRequest request) {
       //1. Find original url using slug for metadata access
        ShortUrl shortUrl = shortUrlRepository.findBySlugAndDeletedAtIsNull(slug)
                .orElseThrow(() -> new UrlNotFoundException("Short Url not found with slug : " + slug)) ;

        //2.find the metadata form link metadata table using hashed original url
        String originalUrl = shortUrl.getOriginalUrl() ;
        String hashedOriginalUrl = DigestUtils.sha256Hex(originalUrl) ;

        Optional<LinkMetadata> linkMetadataOptional = linkMetadataRepository.findByUrlHash(hashedOriginalUrl) ;

        //3. if the metadata is not present then generate the metadata and store it into the db
        if (linkMetadataOptional.isEmpty()) {
            linkMetadataEventProducer.publishLinkMetadataEvent(originalUrl);
            String rootDomain = DomainUtil.extractRootDomain(originalUrl) ;
            LinkMetadata linkMetadata = metadataScraperService
                    .scrape(originalUrl)
                    .orElse(buildGeneralLinkMetadata(hashedOriginalUrl , rootDomain)) ;

            return RedirectPreviewResponse.fromEntity(shortUrl , linkMetadata , frontendBaseUrl);
        }

        //4. if the link metadata is already present
        LinkMetadata linkMetadata = linkMetadataOptional.get() ;

        return RedirectPreviewResponse.fromEntity(shortUrl , linkMetadata , frontendBaseUrl) ;
    }

    private LinkMetadata buildGeneralLinkMetadata(String hashedOriginalUrl , String rootDomain) {
        return LinkMetadata.builder()
                .urlHash(hashedOriginalUrl)
                .title("Link Preview Unavailable")
                .description("No preview could be generated for this destination.")
                .lastScrapedAt(Instant.now())
                .build();
    }

    // RedirectService — pass request to rule engine
    private RedirectData handleCacheMiss(String slug, HttpServletRequest request , boolean isPasswordChecked) {

        ShortUrl url = shortUrlRepository.findBySlugAndDeletedAtIsNull(slug)
                .orElseThrow(() -> new UrlNotFoundException("Slug " + slug + " not found"));

        // ── Safety checks ────────────────────────────────────────────
        if (url.isFlagged()) {
            return RedirectData.unsafe(url.getId().toString(), slug, frontendBaseUrl);
        }
        if (url.getExpiresAt() != null && url.getExpiresAt().isBefore(Instant.now())) {
            return RedirectData.expired(url.getId().toString(), slug, frontendBaseUrl);
        }
        if (url.getMaxClicks() != null && url.getClickCount() >= url.getMaxClicks()) {
            return RedirectData.expired(url.getId().toString(), slug, frontendBaseUrl);
        }

        //Now we check if the url is password protected then redirect user to password window
        if (url.getHashedPassword() != null && !isPasswordChecked) {
            log.info("Link is Password Protected :: handleCacheMiss() :: slug = {}", slug);
            return RedirectData.passwordProtected(url.getId().toString(), slug, frontendBaseUrl);
        }

        // ── Complex rule check ───────────────────────────────────────
        boolean isComplex = url.isHasGeoRule()
                || url.isHasDeviceRule()
                || url.isHasLinkRotation();

        // ── Cache only simple static redirects ───────────────────────
        // Complex rules depend on IP/device — can't cache generically
        if (!isComplex) {
            slugRedirectCacheService.put(
                    url.getSlug(),
                    url.getId().toString(),
                    url.getOriginalUrl(),
                    url.isFlagged(),
                    url.getMaxClicks(),
                    url.getExpiresAt(),
                    url.getClickCount() + 1,
                    isPasswordChecked
            );
        }

        // ── Resolve destination ──────────────────────────────────────
        String destination = isComplex
                ? ruleEngine.resolve(url, request)  // ← pass request
                : url.getOriginalUrl();

        return new RedirectData(
                url.getId().toString(),
                destination,
                302,
                isComplex ? "RULE_ENGINE" : "SIMPLE"
        );
    }

}
