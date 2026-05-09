package com.dishari.in.application.serviceImpl;

import com.dishari.in.application.service.UrlService;
import com.dishari.in.domain.entity.*;
import com.dishari.in.domain.enums.Plan;
import com.dishari.in.domain.enums.UrlStatus;
import com.dishari.in.domain.repository.*;
import com.dishari.in.domain.specification.ShortUrlSpecification;
import com.dishari.in.exception.*;
import com.dishari.in.infrastructure.generator.SlugGeneratorService;
import com.dishari.in.infrastructure.messaging.producer.CreateBulkUrlProducer;
import com.dishari.in.infrastructure.messaging.producer.LinkMetadataEventProducer;
import com.dishari.in.infrastructure.messaging.producer.QrGenerationEventProducer;
import com.dishari.in.infrastructure.qr.QrCodeGeneratorService;
import com.dishari.in.infrastructure.qr.QrCodeStorageService;
import com.dishari.in.web.dto.request.*;
import com.dishari.in.web.dto.response.*;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

import static com.dishari.in.config.AppConstants.ALLOWED_SORT_FIELDS;

@Service
@RequiredArgsConstructor
@Slf4j
public class UrlServiceImpl implements UrlService {

    private final SlugGeneratorService slugGeneratorService;
    private final ShortUrlRepository shortUrlRepository;
    private final PasswordEncoder passwordEncoder ;
    private final UserRepository userRepository;
    private final GeoRuleRepository geoRuleRepository;
    private final DeviceRuleRepository deviceRuleRepository;
    private final LinkRotationRepository linkRotationRepository;
    private final TagRepository tagRepository;
    private final QrGenerationEventProducer qrGenerationEventProducer ;
    private final QrCodeGeneratorService qrCodeGeneratorService;
    private final QrCodeStorageService qrCodeStorageService;
    private final ClickEventRepository clickEventRepository ;
    private final CreateBulkUrlProducer createBulkUrlProducer ;
    private final LinkMetadataEventProducer linkMetadataEventProducer ;

    private static final int MAX_PAGE_SIZE = 50;
    private static final String DEFAULT_SORT_FIELD = "createdAt";

    private List<Plan> premiumPlans = Arrays.asList(Plan.PRO, Plan.ENTERPRISE, Plan.PREMIUM);

    @Value("${app.base-url}")
    private String baseUrl ;
    private final RotationDestinationRepository rotationDestinationRepository;

    //Method that create normal short url
    /**
     * @param email // user email
     * @param request // Request to create normal short url for free user
     * @return NormalUrlResponse
     */
    @Override
    @Transactional
    public NormalUrlResponse createNormalUrl(String email, CreateNormalUrlRequest request) {
        User user = userRepository.findByEmailAndDeletedAtIsNull(email).orElseThrow(() -> new UserNotFoundException("User not found."));

        //Before generating new short url first we check that the user has already created the short url for the original long url
        Optional<ShortUrl> existingShortUrl = shortUrlRepository.findByUserAndOriginalUrlAndStatusAndDeletedAtIsNull(user, request.originalUrl(), UrlStatus.ACTIVE);
        if (existingShortUrl.isPresent()) {
            return NormalUrlResponse.fromEntity(existingShortUrl.get(), baseUrl , true);
        }

        //generator service that generate the short slug by using snowflake id generation technique
        String slug = slugGeneratorService.generateSlug() ;

        ShortUrl shortUrl = ShortUrl.builder()
                .originalUrl(request.originalUrl())
                .slug(slug)
                .title(request.title())
                .user(user)
                .expiresAt(request.expiresAt())
                .maxClicks(request.maxClicks())
                .status(UrlStatus.ACTIVE)
                .build();

        //now we are set the password if user want to set the password
        if (request.password() != null && !request.password().trim().isEmpty()) {
            shortUrl.setHashedPassword(passwordEncoder.encode(request.password()));
        }

        //We store utm data if the utm in request is not null
        if (request.utm() != null) {
            shortUrl.setUtmSource(request.utm().utmSource());
            shortUrl.setUtmMedium(request.utm().utmMedium());
            shortUrl.setUtmCampaign(request.utm().utmCampaign());
            shortUrl.setUtmContent(request.utm().utmContent());
            shortUrl.setUtmTerm(request.utm().utmTerm());
        }

        //TODO: Check url safe or not(flagged)

        ShortUrl savedShortUrl = shortUrlRepository.save(shortUrl) ;

        //NOTE: If user has premium than qr code will be generated
        if (premiumPlans.contains(user.getPlan())) {
            String shortUrlString = baseUrl + "/" + savedShortUrl.getSlug() ;
            qrGenerationEventProducer.publishQrGenerationEvent(savedShortUrl.getId() , savedShortUrl.getSlug() , shortUrlString  , savedShortUrl.getUser().getId() ,300 , "#000000" , "#FFFFFF" , null , "PNG");
        }

        //Publish a link metadata event to create link metadata
        linkMetadataEventProducer.publishLinkMetadataEvent(request.originalUrl());

        return NormalUrlResponse.fromEntity(shortUrl, baseUrl , false);
    }

    /**
     * Method that create custom short url for paid user
     *
     * @param email   // User email
     * @param request // CreateCustomUrlRequest DTO
     * @return // NormalUrlResponse
     */
    @Override
    @Transactional
    public CustomUrlResponse createCustomUrl(String email, CreateCustomUrlRequest request) {
        User user = userRepository.findByEmailAndDeletedAtIsNull(email)
                .orElseThrow(() -> new UserNotFoundException("User not found."));

        // Validation
        if (shortUrlRepository.existsBySlugAndStatusAndDeletedAtIsNull(request.customSlug(), UrlStatus.ACTIVE)) {
            throw new SlugAlreadyTakenException("Custom slug already exists.");
        }
        slugGeneratorService.validateCustomSlug(request.customSlug());

        // 1. Create and Save Root
        ShortUrl savedShortUrl = shortUrlRepository.save(createCustomShortUrl(user, request));

        String shortUrlString = baseUrl + "/" + savedShortUrl.getSlug() ;
        qrGenerationEventProducer.publishQrGenerationEvent(savedShortUrl.getId() , savedShortUrl.getSlug() , shortUrlString  , savedShortUrl.getUser().getId() ,300 , "#000000" , "#FFFFFF" , null , "PNG");
      

        //TODO: Check url safe or not(flagged)

        // 2. Fetch or Create Collections (Optimized with saveAll)
        Set<Tag> tags = storeTag(savedShortUrl, request.tags());
        Set<GeoRule> geoRules = storeGeoRule(savedShortUrl, request.geoRules());
        Set<DeviceRule> deviceRules = storeDeviceRule(savedShortUrl, request.deviceRules());

        LinkRotation linkRotation = null;
        if (request.linkRotation() != null) {
            linkRotation = storeLinkRotation(savedShortUrl, request.linkRotation());
        }

        //Publish a link metadata event to create link metadata
        linkMetadataEventProducer.publishLinkMetadataEvent(request.originalUrl());
        // 3. Return the rich response
        return CustomUrlResponse.fromEntity(
                savedShortUrl,
                tags,
                geoRules,
                deviceRules,
                linkRotation,
                baseUrl
        );
    }


    /**
     * Get user urls
     * @param filter // Url Request filter that contains (q, status, from, to, country, deviceType, tag)
     * @param principal // User entity
     * @param page // page number
     * @param size // size of page data
     * @param sortBy // data is sorted by
     * @param sortDir // direction of sorting
     * @return // PaginatedResponse of ShortUrlResponse
     */
    @Override
    @Transactional(readOnly = true)
    public PaginatedResponse<ShortUrlResponse> getUserUrls(
            UrlFilterRequest filter,
            User principal,
            int page,
            int size,
            String sortBy,
            String sortDir) {

        //It is a critical safety point so we must check user by using email
        String email = principal.getEmail() ;

        User user = userRepository.findByEmailAndDeletedAtIsNull(email)
                .orElseThrow(() -> new UserNotFoundException("User not found."));

        //----Sanitize inputs
        int safePage = Math.max(0 , page) ;
        int safeSize = Math.min(size , MAX_PAGE_SIZE) ;
        String safeSortBy = resolveSortField(sortBy , user);
        String safeSortDir = sortDir != null ? sortDir : "desc" ;

        Sort.Direction direction = safeSortDir.equals("asc")
                ? Sort.Direction.ASC
                : Sort.Direction.DESC ;

        Pageable pageable = PageRequest.of(safePage , safeSize , Sort.by(direction , safeSortBy)) ;

        Specification<ShortUrl> specification = ShortUrlSpecification.buildSpec(filter , user) ;

        Page<ShortUrl> resultPage = shortUrlRepository.findAll(specification , pageable) ;

        log.debug("URL query: userId={} plan={} hasPremium={} " +
                        "total={} page={} size={}",
                user.getId(),
                user.getPlan(),
                user.isHasPremium(),
                resultPage.getTotalElements(),
                safePage,
                safeSize
        );
        return PaginatedResponse.of(resultPage , url -> ShortUrlResponse.fromEntity(url , baseUrl));
    }

    @Override
    @Transactional(readOnly = true)
    public UrlDetailResponse getUrlDetail(User principal, String id) {
        UUID urlUUID = UUID.fromString(id) ;
        ShortUrl shortUrl = shortUrlRepository.findByIdWithTagsAndDeletedAtIsNull(urlUUID)
                .orElseThrow(() -> new UrlNotFoundException("The Url with id : " + id + " is not present." ));

        //Check if user is the owner of the url
        if (!UrlSecurityService.isOwner(principal , shortUrl)) {
            throw new UserNotOwnException("The Url with id : " + id + " is not own by you.") ;
        }

        //Now we find all the geo rule , Device rule and LinkRotation
        List<GeoRule> geoRules = geoRuleRepository.findByShortUrlId(urlUUID) ;
        List<DeviceRule> deviceRules = deviceRuleRepository.findByShortUrlId(urlUUID) ;
        LinkRotation linkRotation = linkRotationRepository.findActiveRotationWithDestinations(urlUUID)
                .orElse(null) ;

        return UrlDetailResponse.fromEntity(shortUrl , baseUrl , geoRules , deviceRules , linkRotation);
    }

    @Override
    @Transactional
    public ShortUrlUpdateResponse updateUrlData(User principal, ShortUrlUpdateRequest updateRequest, String id) {

        UUID urlUUID = UUID.fromString(id) ;
        ShortUrl shortUrl = shortUrlRepository.findByIdAndDeletedAtIsNull(urlUUID)
                .orElseThrow(() -> new UrlNotFoundException("The Url with id : " + id + " is not present." ));

        //Check if user is the owner of the url
        if (!UrlSecurityService.isOwner(principal , shortUrl)) {
            throw new UserNotOwnException("The Url with id : " + id + " is not own by you.") ;
        }

        //TODO: Security/Safety Check
        if (!shortUrl.getOriginalUrl().equals(updateRequest.originalUrl())) {
            // validateUrlSafety(updateRequest.originalUrl()); // Your logic for flagged/malicious URLs
        }

        updateFromRequest(shortUrl , updateRequest) ;
        ShortUrl savedShortUrl = shortUrlRepository.save(shortUrl) ;

        //TODO :Implementation for redis cache evict

        return ShortUrlUpdateResponse.fromEntity(savedShortUrl) ;
    }

    @Override // Implement soft delete
    @Transactional
    public MessageResponse deleteShortUrl(User principal, String id) {
        UUID idUUID = UUID.fromString(id) ;
        ShortUrl shortUrl = shortUrlRepository.findById(idUUID)
                .orElseThrow(() -> new UrlNotFoundException("The Url with id : " + id + " is not present.")) ;

        //Validation that the url is owned by the user or not
        if (!UrlSecurityService.isOwner(principal , shortUrl)) {
            throw new UserNotOwnException("The Url with id : " + id + " is not own by you.") ;
        }

        shortUrl.setDeletedAt(Instant.now());
        shortUrlRepository.save(shortUrl) ;

        return MessageResponse.builder()
                .message("Url deleted successfully.")
                .status(HttpStatus.OK.value())
                .timestamp(Instant.now())
                .build();
    }

    @Override
    public UpdateUrlStatusResponse updateStatus(User principal, UpdateUrlStatusRequest updateRequest, String id) {

        ShortUrl shortUrl = shortUrlRepository.findByIdAndDeletedAtIsNull(UUID.fromString(id))
                .orElseThrow(() -> new UrlNotFoundException("The Url with id : " + id + " is not present.")) ;

        //Check if user is the owner of the url
        if (!UrlSecurityService.isOwner(principal , shortUrl)) {
            throw new UserNotOwnException("The Url with id : " + id + " is not own by you.") ;
        }

        //If status is same then no need to change
        if (shortUrl.getStatus() == updateRequest.status()) {
            return UpdateUrlStatusResponse.fromEntity(shortUrl) ;
        }

        shortUrl.setStatus(updateRequest.status());
        ShortUrl savedShortUrl = shortUrlRepository.save(shortUrl) ;

        //TODO: Evict the cache for this url

        return UpdateUrlStatusResponse.fromEntity(savedShortUrl);
    }

    /**
     * Generates or returns existing QR code bytes with user-controlled regeneration.
     *
     * @param shortUrlId   UUID of the ShortUrl
     * @param size         Width/Height of QR code
     * @param fgColor      Foreground color
     * @param bgColor      Background color
     * @param logoUrl      Optional logo URL
     * @param format       PNG or SVG
     * @param regenerate   true = force delete old + generate new QR
     * @param principal    Current authenticated user
     * @return QR code as byte array
     */
    @Transactional
    public Resource getQrCodeResource(
            UUID shortUrlId,
            int size,
            String fgColor,
            String bgColor,
            String logoUrl,
            String format,
            boolean regenerate,
            User principal) {

        ShortUrl shortUrl = shortUrlRepository.findById(shortUrlId)
                .orElseThrow(() -> new UrlNotFoundException("Short URL not found"));

        // Ownership check
        if (!shortUrl.getUser().getId().equals(principal.getId())) {
            throw new AccessDeniedException("Access denied");
        }

        String slug = shortUrl.getSlug();
        String fileName = qrCodeGeneratorService.generateFileName(slug, size, fgColor, bgColor , logoUrl , format);

        // If regenerate is true → delete old file
        if (regenerate) {
            log.info("User requested QR regeneration for slug: {}", slug);
            if (shortUrl.getQrCodeUrl() != null) {
                qrCodeStorageService.deleteFile(shortUrl.getQrCodeUrl());
            }
            shortUrlRepository.updateQrCodeUrl(shortUrl.getId(), null);
        }

        // Try to serve existing file
        if (!regenerate && qrCodeStorageService.exists(fileName)) {
            Resource resource = qrCodeStorageService.getQrCodeAsResource(fileName);
            if (resource != null) {
                log.debug("Serving existing QR from storage: {}", fileName);
                return resource;
            }
        }

        // Generate new QR code
        log.info("Generating new QR code for slug: {}", slug);

        QrCodeGeneratorService.QrCodeGeneratedResult result =
                qrCodeGeneratorService.generateAndSave(
                        shortUrl.getOriginalUrl(),
                        slug,
                        size,
                        fgColor,
                        bgColor,
                        logoUrl,
                        format
                );

        // Update database with new QR path
        shortUrlRepository.updateQrCodeUrl(shortUrl.getId(), result.fileUrl());

        // Return the newly generated file as Resource
        return qrCodeStorageService.getQrCodeAsResource(
                qrCodeGeneratorService.generateFileName(slug, size, fgColor , bgColor , logoUrl, format)
        );
    }

    @Override
    @Transactional(readOnly = true)
    public UrlAnalyticsResponse getAnalytics(User principal, String shortUrlId, AnalyticsFilterRequest filter) {
        // ── 1. Fetch and validate ownership ─────────────────────
        UUID id = parseUUID(shortUrlId);

        ShortUrl shortUrl = shortUrlRepository
                .findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new UrlNotFoundException(
                        "Short URL not found: " + shortUrlId));

        if (!shortUrl.getUser().getId().equals(principal.getId())) {
            throw new UnauthorizedException(
                    "You don't have access to analytics for this URL.");
        }

        // ── 2. Resolve date range ────────────────────────────────
        Instant from     = filter.resolvedFrom();
        Instant to       = filter.resolvedTo();
        String  groupBy  = filter.resolvedGroupBy();
        int     limit    = filter.limit();

        log.debug(
                "Analytics query: id={} from={} to={} groupBy={}",
                id, from, to, groupBy);

        // Building the Summary
        AnalyticsSummary summary = buildSummary(id, from, to);

        // Build trends based on time series
        List<ClickTrend> trends = buildTrends(id, from, to, groupBy);


        List<CountryStats> topCountries = buildCountryStats(id, from, to, limit);
        List<DeviceStats>  topDevices   = buildDeviceStats(id, from, to);
        List<BrowserStats> topBrowsers  = buildBrowserStats(id, from, to, limit);
        List<ReferrerStats> topReferrers = buildReferrerStats(id, from, to, limit);

        String periodDescription = buildPeriodDescription(from, to, groupBy);

        return UrlAnalyticsResponse.from(
                shortUrl,
                summary,
                trends,
                topCountries,
                topDevices,
                topBrowsers,
                topReferrers,
                periodDescription
        );
    }

    @Override
    @Transactional
    public MessageResponse createBulkUrl(User principal, CreateBulkUrlRequest request) {
        //Verify the user
        User user = userRepository.findByEmailAndDeletedAtIsNull(principal.getEmail())
                .orElseThrow(() -> new UserNotFoundException("User not found with email : " + principal.getEmail())) ;

        //Send event into the Kafka Messaging Queue
        createBulkUrlProducer.publishBulkUrlEvent(user , request);

        return MessageResponse.builder()
                .message("Bulk URL creation request processed successfully. You will receive an email once processing is complete.")
                .timestamp(Instant.now())
                .status(HttpStatus.ACCEPTED.value())
                .build();
    }

    @Override
    @Transactional
    //NOTE: For bulk url we generate qr code when the user want not at the time of shortUrl creation
    public BulkUrlResponse createShortUrlFormBulk(User user, BulkUrlRequest request) {
        //Before generating new short url first we check that the user has already created the short url for the original long url
        Optional<ShortUrl> existingShortUrl = shortUrlRepository.findByUserAndOriginalUrlAndStatusAndDeletedAtIsNull(user, request.originalUrl(), UrlStatus.ACTIVE);
        if (existingShortUrl.isPresent()) {
            return BulkUrlResponse.fromEntity(existingShortUrl.get(), baseUrl , true);
        }

        ShortUrl shortUrl = buildShortUrlFromBulkUrlRequest(user , request) ;

        //TODO: Check url safe or not(flagged)

        ShortUrl savedShortUrl = shortUrlRepository.save(shortUrl) ;

        return BulkUrlResponse.fromEntity(savedShortUrl , baseUrl , false);
    }

    @Override
    @Transactional(readOnly = true)
    public PaginatedResponse<ClickEventResponse> getRawClicks(int page, int size, String sortBy, String sortDirection, User principal, String id) {
        ShortUrl shortUrl = shortUrlRepository.findByIdAndDeletedAtIsNull(parseUUID(id))
                .orElseThrow(() -> new UrlNotFoundException("Short URL not found with id: " + id));

        //First we check ownership for the shortUrl
        if (!UrlSecurityService.isOwner(principal , shortUrl)) {
            throw new UserNotOwnException("You don't have access to analytics for this URL.");
        }

        Sort.Direction direction = sortDirection.equalsIgnoreCase("asc") ? Sort.Direction.ASC : Sort.Direction.DESC;

        String safeSortBy = resolveSortField(sortBy , principal) ;
        Pageable pageable = PageRequest.of(page , size , Sort.by(direction , safeSortBy)) ;

        Page<ClickEvent> clickEventPage = clickEventRepository.findAllByShortUrl(shortUrl , pageable) ;

        return PaginatedResponse.of(clickEventPage , ClickEventResponse::fromEntity);
    }


    //Method that create shortUrl from bulk url request
    private ShortUrl buildShortUrlFromBulkUrlRequest(User user , BulkUrlRequest request) {
        //generator service that generate the short slug by using snowflake id generation technique
        String slug = slugGeneratorService.generateSlug() ;

        ShortUrl shortUrl = ShortUrl.builder()
                .originalUrl(request.originalUrl())
                .slug(slug)
                .title(request.title())
                .user(user)
                .expiresAt(request.expiresAt())
                .maxClicks(request.maxClicks())
                .status(UrlStatus.ACTIVE)
                .build();

        //now we are set the password if user want to set the password
        if (request.password() != null && !request.password().trim().isEmpty()) {
            shortUrl.setHashedPassword(passwordEncoder.encode(request.password()));
        }

        //We store utm data if the utm in request is not null
        if (request.utm() != null) {
            shortUrl.setUtmSource(request.utm().utmSource());
            shortUrl.setUtmMedium(request.utm().utmMedium());
            shortUrl.setUtmCampaign(request.utm().utmCampaign());
            shortUrl.setUtmContent(request.utm().utmContent());
            shortUrl.setUtmTerm(request.utm().utmTerm());
        }

        return shortUrl ;
    }

    //Method that is used to create shortUrl called by Kafka Event

    /**
     * Create custom short url
     * @param user // User entity
     * @param request // CreateCustomUrlRequest DTO
     * @return // ShortUrl entity
     */
    private ShortUrl createCustomShortUrl(User user , CreateCustomUrlRequest request) {
        ShortUrl shortUrl =  ShortUrl.builder()
                .slug(request.customSlug())
                .originalUrl(request.originalUrl())
                .title(request.title())
                .expiresAt(request.expiresAt())
                .maxClicks(request.maxClicks())
                .status(UrlStatus.ACTIVE)
                .user(user)
                .build() ;

        //Set utm data if the utm in request is not null
        if (request.utm() != null ) {
            addUtmDataInShortUrl(shortUrl , request) ;
        }

        //If security password is available
        if (request.password() != null && !request.password().trim().isEmpty()) {
            shortUrl.setHashedPassword(passwordEncoder.encode(request.password()));
        }

        return shortUrl ;
    }

    /**
     * Add utm data in short url
     * @param shortUrl // Takes short url entity object
     * @param request // Takes CreateCustomUrlRequest object
     */
    private void addUtmDataInShortUrl(ShortUrl shortUrl , CreateCustomUrlRequest request) {
        shortUrl.setUtmSource(request.utm().utmSource());
        shortUrl.setUtmMedium(request.utm().utmMedium());
        shortUrl.setUtmCampaign(request.utm().utmCampaign());
        shortUrl.setUtmContent(request.utm().utmContent());
        shortUrl.setUtmTerm(request.utm().utmTerm());
    }

    /**
     * Convert tag request to tag entity
     * @param request // TagRequest DTO
     * @return // Tag entity
     */
    private Tag convertTagRequestToTag(TagRequest request) {
        return Tag.builder()
                .color(request.color())
                .name(request.name())
                .build() ;
    }
    private Set<Tag> storeTag (ShortUrl shortUrl , Set<TagRequest> tags) {
        if (tags != null && !tags.isEmpty()) {
            List<Tag> tagsEntity = tags.stream()
                    .map(this::convertTagRequestToTag)
                    .toList();

            List<Tag> savedTags = tagRepository.saveAll(tagsEntity); // Batch save
            shortUrl.setTags(new HashSet<>(savedTags));
            return new HashSet<>(tagsEntity) ;
        }
        return new HashSet<>() ;
    }
    /**
     * Store geo rule
     * @param shortUrl // ShortUrl entity
     * @param requestSet // Set of CreateGeoRuleRequest DTO
     */
    private Set<GeoRule> storeGeoRule(ShortUrl shortUrl , Set<CreateGeoRuleRequest> requestSet) {
        if (requestSet != null && !requestSet.isEmpty()) {
            shortUrl.setHasGeoRule(true);
            List<GeoRule> geoRules = requestSet.stream()
                    .map(rule -> convertGeoRuleRequestToGeoRule(shortUrl ,rule))
                    .collect(Collectors.toList());
            List<GeoRule> savedGeoRules =  geoRuleRepository.saveAll(geoRules); // Batch save
            return new HashSet<>(savedGeoRules) ;
        }
        return new HashSet<>() ;
    }

    private GeoRule convertGeoRuleRequestToGeoRule(ShortUrl shortUrl , CreateGeoRuleRequest request) {
        return GeoRule.builder()
                .shortUrl(shortUrl)
                .countryCode(request.countryCode())
                .destinationUrl(request.destinationUrl())
                .isDefault(request.isDefault())
                .priority(request.priority() != null ? request.priority() : 1)
                .build() ;
    }

    /**
     * Store device rule
     * @param shortUrl // ShortUrl entity
     * @param requestSet // Set of CreateDeviceRuleRequest DTO
     */
    private Set<DeviceRule> storeDeviceRule(ShortUrl shortUrl , Set<CreateDeviceRuleRequest> requestSet ) {

        if (requestSet != null && !requestSet.isEmpty()) {
            shortUrl.setHasDeviceRule(true);
            List<DeviceRule> deviceRules = requestSet.stream()
                    .map(r -> convertDeviceRuleRequestToDeviceRule(shortUrl ,r))
                    .collect(Collectors.toList());
            List<DeviceRule> savedDeviceRules = deviceRuleRepository.saveAll(deviceRules); // Batch save
            return new HashSet<>(savedDeviceRules) ;
        }
        return new HashSet<>() ;
    }

    private DeviceRule convertDeviceRuleRequestToDeviceRule(ShortUrl shortUrl, CreateDeviceRuleRequest request) {
        return DeviceRule.builder()
                .shortUrl(shortUrl)
                .deviceType(request.deviceType())
                .destinationUrl(request.destinationUrl())
                .isDefault(request.isDefault())
                .build();
    }

    /**
     * Store link rotation
     * @param shortUrl // ShortUrl entity
     * @param requestSet // CreateLinkRotationRequest DTO
     */
    private LinkRotation storeLinkRotation(ShortUrl shortUrl , CreateLinkRotationRequest requestSet){
        shortUrl.setHasLinkRotation(true);
        LinkRotation linkRotation = LinkRotation.builder()
                .rotationStrategy(requestSet.rotationStrategy())
                .shortUrl(shortUrl)
                .build() ;

        LinkRotation savedLinkRotation = linkRotationRepository.save(linkRotation) ;
        List<RotationDestination> rotationDestinations = requestSet.rotationDestinations().stream()
                .map(request -> convertRotationDestinationRequestToRotationDestination(savedLinkRotation ,request))
                .toList() ;

        List<RotationDestination> savedRotationDestinations = rotationDestinationRepository.saveAll(rotationDestinations) ;
        linkRotation.setRotationDestinations(savedRotationDestinations) ;
        return linkRotationRepository.save(linkRotation) ;
    }

    /**
     * Convert rotation destination request to rotation destination entity
     * @param request // CreateRotationDestinationRequest DTO
     * @return // RotationDestination entity
     */
    private RotationDestination convertRotationDestinationRequestToRotationDestination(LinkRotation linkRotation ,CreateRotationDestinationRequest request) {
        return RotationDestination.builder()
                .linkRotation(linkRotation)
                .destinationUrl(request.destinationUrl())
                .weight(request.weight() == null ? 1 : request.weight())
                .position(request.position() == null ? 1 : request.position())
                .activeFrom(request.activeFrom())
                .activeTo(request.activeTo())
                .active(request.active())
                .build() ;
    }

    private String resolveSortField(String sortBy, User user) {

        // ── No sort field provided → use default ─────────────────────
        if (sortBy == null || sortBy.isBlank()) {
            return DEFAULT_SORT_FIELD;
        }

        // ── Safety check — whitelist only ────────────────────────────
        // Unknown field → reject with 400 instead of silently falling back
        // switch exhausts all valid cases, default → throw
        return switch (sortBy.trim().toLowerCase()) {
            case "createdat"  -> "createdAt";
            case "updatedat"  -> "updatedAt";
            case "expiresat"  -> "expiresAt";
            case "clickcount" -> "maxClicks";
            case "title"      -> "title";
            case "slug"       -> "slug";
            default           -> {
                log.warn(
                        "Invalid sortBy attempted: userId={} sortBy={}",
                        user.getId(), sortBy
                );
                throw new InvalidSortFieldException(
                        "Invalid sort field: '" + sortBy + "'. " +
                                "Allowed fields (case-insensitive): " + ALLOWED_SORT_FIELDS
                );
            }
        };
    }

    private void updateFromRequest(ShortUrl shortUrl , ShortUrlUpdateRequest updateRequest) {
        shortUrl.setOriginalUrl(updateRequest.originalUrl() != null ? updateRequest.originalUrl() : shortUrl.getOriginalUrl());
        shortUrl.setTitle(updateRequest.title() != null ? updateRequest.title() : shortUrl.getTitle());
        shortUrl.setExpiresAt(updateRequest.expiresAt() != null ? updateRequest.expiresAt() : shortUrl.getExpiresAt());
        shortUrl.setMaxClicks(updateRequest.maxClicks() != null ? updateRequest.maxClicks() : shortUrl.getMaxClicks());
        shortUrl.setHashedPassword(updateRequest.password() != null ? passwordEncoder.encode(updateRequest.password()) : shortUrl.getHashedPassword());
        if (updateRequest.utm() != null) {
            shortUrl.setUtmCampaign(updateRequest.utm().utmCampaign() != null ? updateRequest.utm().utmCampaign().trim() : shortUrl.getUtmCampaign());
            shortUrl.setUtmMedium(updateRequest.utm().utmMedium() != null ? updateRequest.utm().utmMedium().trim() : shortUrl.getUtmMedium());
            shortUrl.setUtmSource(updateRequest.utm().utmSource() != null ? updateRequest.utm().utmSource().trim() : shortUrl.getUtmSource());
            shortUrl.setUtmTerm(updateRequest.utm().utmTerm() != null ? updateRequest.utm().utmTerm().trim() : shortUrl.getUtmTerm());
            shortUrl.setUtmContent(updateRequest.utm().utmContent() != null ? updateRequest.utm().utmContent().trim() : shortUrl.getUtmContent());
        }
    }

    // ── Summary ──────────────────────────────────────────────────
    private AnalyticsSummary buildSummary(
            UUID id, Instant from, Instant to) {

        long totalClicks  = clickEventRepository
                .countTotalClicks(id, from, to);
        long uniqueClicks = clickEventRepository
                .countUniqueClicks(id, from, to);
        Instant firstClick = clickEventRepository
                .findFirstClickAt(id);
        Instant lastClick  = clickEventRepository
                .findLastClickAt(id);

        double avgPerDay = calculateAverageClicksPerDay(
                totalClicks, from, to);

        return new AnalyticsSummary(
                totalClicks,
                uniqueClicks,
                firstClick,
                lastClick,
                avgPerDay
        );
    }

    // ── Time Series ──────────────────────────────────────────────
    private List<ClickTrend> buildTrends(
            UUID id, Instant from, Instant to, String groupBy) {

        List<Object[]> raw = switch (groupBy) {
            case "HOUR"  -> clickEventRepository
                    .findHourlyTrends(id, from, to);
            case "WEEK"  -> clickEventRepository
                    .findWeeklyTrends(id, from, to);
            case "MONTH" -> clickEventRepository
                    .findMonthlyTrends(id, from, to);
            default      -> clickEventRepository
                    .findDailyTrends(id, from, to);
        };

        return raw.stream()
                .map(row -> new ClickTrend(
                        String.valueOf(row[0]),              // period
                        ((Number) row[1]).longValue(),       // totalClicks
                        ((Number) row[2]).longValue()        // uniqueClicks
                ))
                .toList();
    }

    // ── Country Breakdown ────────────────────────────────────────
    private List<CountryStats> buildCountryStats(
            UUID id, Instant from, Instant to, int limit) {

        List<Object[]> raw = clickEventRepository
                .findTopCountries(id, from, to, limit);

        long total = raw.stream()
                .mapToLong(r -> ((Number) r[2]).longValue())
                .sum();

        return raw.stream()
                .map(row -> {
                    long clicks = ((Number) row[2]).longValue();
                    return new CountryStats(
                            String.valueOf(row[0]),              // country
                            String.valueOf(row[1]),              // countryName
                            clicks,
                            calculatePercentage(clicks, total)
                    );
                })
                .toList();
    }

    // ── Device Breakdown ─────────────────────────────────────────
    private List<DeviceStats> buildDeviceStats(
            UUID id, Instant from, Instant to) {

        List<Object[]> raw = clickEventRepository
                .findDeviceBreakdown(id, from, to);

        long total = raw.stream()
                .mapToLong(r -> ((Number) r[1]).longValue())
                .sum();

        return raw.stream()
                .map(row -> {
                    long clicks = ((Number) row[1]).longValue();
                    return new DeviceStats(
                            String.valueOf(row[0]),
                            clicks,
                            calculatePercentage(clicks, total)
                    );
                })
                .toList();
    }

    // ── Browser Breakdown ────────────────────────────────────────
    private List<BrowserStats> buildBrowserStats(
            UUID id, Instant from, Instant to, int limit) {

        List<Object[]> raw = clickEventRepository
                .findTopBrowsers(id, from, to, limit);

        long total = raw.stream()
                .mapToLong(r -> ((Number) r[1]).longValue())
                .sum();

        return raw.stream()
                .map(row -> {
                    long clicks = ((Number) row[1]).longValue();
                    return new BrowserStats(
                            String.valueOf(row[0]),
                            clicks,
                            calculatePercentage(clicks, total)
                    );
                })
                .toList();
    }

    // ── Referrer Breakdown ───────────────────────────────────────
    private List<ReferrerStats> buildReferrerStats(
            UUID id, Instant from, Instant to, int limit) {

        List<Object[]> raw = clickEventRepository
                .findTopReferrers(id, from, to, limit);

        long total = raw.stream()
                .mapToLong(r -> ((Number) r[1]).longValue())
                .sum();

        return raw.stream()
                .map(row -> {
                    long clicks = ((Number) row[1]).longValue();
                    return new ReferrerStats(
                            String.valueOf(row[0]),
                            clicks,
                            calculatePercentage(clicks, total)
                    );
                })
                .toList();
    }

    // ── Helpers ──────────────────────────────────────────────────

    private double calculateAverageClicksPerDay(
            long totalClicks, Instant from, Instant to) {
        long days = ChronoUnit.DAYS.between(from, to);
        if (days == 0) days = 1; // avoid division by zero
        return Math.round((double) totalClicks / days * 100.0) / 100.0;
    }

    private double calculatePercentage(long part, long total) {
        if (total == 0) return 0.0;
        return Math.round((double) part / total * 10000.0) / 100.0;
        // e.g. 33.33 not 33.333333
    }

    private String buildPeriodDescription(Instant from, Instant to, String groupBy) {
        return String.format(
                "From %s to %s grouped by %s",
                from.toString().substring(0, 10),
                to.toString().substring(0, 10),
                groupBy
        );
    }

    private UUID parseUUID(String id) {
        try {
            return UUID.fromString(id);
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException(
                    "Invalid URL id format: " + id);
        }
    }
}
