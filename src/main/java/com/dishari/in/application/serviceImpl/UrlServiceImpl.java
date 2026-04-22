package com.dishari.in.application.serviceImpl;

import com.dishari.in.application.service.UrlService;
import com.dishari.in.domain.entity.ShortUrl;
import com.dishari.in.domain.entity.User;
import com.dishari.in.domain.enums.UrlStatus;
import com.dishari.in.domain.repository.ShortUrlRepository;
import com.dishari.in.domain.repository.UserRepository;
import com.dishari.in.exception.CustomSlugAlreadyExistsException;
import com.dishari.in.exception.UserNotFoundException;
import com.dishari.in.infrastructure.generator.SlugGeneratorService;
import com.dishari.in.web.dto.request.CreateCustomUrlRequest;
import com.dishari.in.web.dto.request.CreateNormalUrlRequest;
import com.dishari.in.web.dto.response.NormalUrlResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UrlServiceImpl implements UrlService {

    private final SlugGeneratorService slugGeneratorService;
    private final ShortUrlRepository shortUrlRepository;
    private final PasswordEncoder passwordEncoder ;
    private final UserRepository userRepository;

    @Value("${app.base-url}")
    private String baseUrl ;

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

        shortUrlRepository.save(shortUrl) ;

        return NormalUrlResponse.fromEntity(shortUrl, baseUrl , false);
    }

    @Override
    @Transactional
    public NormalUrlResponse createCustomUrl(String email, CreateCustomUrlRequest request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("User not found."));

        //Before generating new short url first we check that the user has already created the short url for the original long url
        ShortUrl existingCustomSlug = shortUrlRepository.findBySlugAndStatus(request.customSlug() , UrlStatus.ACTIVE)
                .orElseThrow(() -> new CustomSlugAlreadyExistsException("Custom slug already exists.")) ;




        return null;
    }

    private ShortUrl createCustomShortUrl(User user , CreateCustomUrlRequest request) {
        ShortUrl shortUrl =  ShortUrl.builder()
                .slug(request.customSlug())
                .originalUrl(request.originalUrl())
                .title(request.title())
                .user(user)
                .expiresAt(request.expiresAt())
                .maxClicks(request.maxClicks())
                .status(UrlStatus.ACTIVE)
                .user(user)
                .build() ;

        //Set utm data if the utm in request is not null
        if (request.utm() != null ) {
            shortUrl.setUtmSource(request.utm().utmSource());
            shortUrl.setUtmMedium(request.utm().utmMedium());
            shortUrl.setUtmCampaign(request.utm().utmCampaign());
            shortUrl.setUtmContent(request.utm().utmContent());
            shortUrl.setUtmTerm(request.utm().utmTerm());
        }

        //If security password is available
        if (request.password() != null && request.password().trim().isEmpty()) {
            shortUrl.setHashedPassword(passwordEncoder.encode(request.password()));
        }


    }
}
