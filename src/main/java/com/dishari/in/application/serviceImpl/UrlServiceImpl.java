package com.dishari.in.application.serviceImpl;

import com.dishari.in.application.service.UrlService;
import com.dishari.in.domain.entity.*;
import com.dishari.in.domain.enums.UrlStatus;
import com.dishari.in.domain.repository.*;
import com.dishari.in.exception.SlugAlreadyTakenException;
import com.dishari.in.exception.UserNotFoundException;
import com.dishari.in.infrastructure.generator.SlugGeneratorService;
import com.dishari.in.web.dto.request.*;
import com.dishari.in.web.dto.response.CustomUrlResponse;
import com.dishari.in.web.dto.response.NormalUrlResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UrlServiceImpl implements UrlService {

    private final SlugGeneratorService slugGeneratorService;
    private final ShortUrlRepository shortUrlRepository;
    private final PasswordEncoder passwordEncoder ;
    private final UserRepository userRepository;
    private final GeoRuleRepository geoRuleRepository;
    private final DeviceRuleRepository deviceRuleRepository;
    private final LinkRotationRepository linkRotationRepository;
    private final TagRepository tagRepository;

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
        User user = userRepository.findByEmail(email).orElseThrow(() -> new UserNotFoundException("User not found."));

        //Before generating new short url first we check that the user has already created the short url for the original long url
        Optional<ShortUrl> existingShortUrl = shortUrlRepository.findByUserAndOriginalUrlAndStatus(user, request.originalUrl(), UrlStatus.ACTIVE);
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

        //TODO: QR code generator
        //TODO: Check url safe or not(flagged)

        shortUrlRepository.save(shortUrl) ;

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
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("User not found."));

        // Validation
        if (shortUrlRepository.existsBySlugAndStatus(request.customSlug(), UrlStatus.ACTIVE)) {
            throw new SlugAlreadyTakenException("Custom slug already exists.");
        }
        slugGeneratorService.validateCustomSlug(request.customSlug());

        // 1. Create and Save Root
        ShortUrl savedShortUrl = shortUrlRepository.save(createCustomShortUrl(user, request));

        // 2. Fetch or Create Collections (Optimized with saveAll)
        Set<Tag> tags = storeTag(savedShortUrl, request.tags());
        Set<GeoRule> geoRules = storeGeoRule(savedShortUrl, request.geoRules());
        Set<DeviceRule> deviceRules = storeDeviceRule(savedShortUrl, request.deviceRules());

        LinkRotation linkRotation = null;
        if (request.linkRotation() != null) {
            linkRotation = storeLinkRotation(savedShortUrl, request.linkRotation());
        }

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
                .priority(request.priority())
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
                .weight(request.weight())
                .position(request.position())
                .activeFrom(request.activeFrom())
                .activeTo(request.activeTo())
                .active(request.active())
                .build() ;
    }
}
