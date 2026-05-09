package com.dishari.in.application.serviceImpl;

import com.dishari.in.application.service.AuthService;
import com.dishari.in.domain.entity.RefreshToken;
import com.dishari.in.domain.entity.User;
import com.dishari.in.domain.enums.SocialProvider;
import com.dishari.in.domain.enums.UserStatus;
import com.dishari.in.domain.repository.RefreshTokenRepository;
import com.dishari.in.domain.repository.UserRepository;
import com.dishari.in.exception.*;
import com.dishari.in.infrastructure.cache.EmailVerificationCacheService;
import com.dishari.in.infrastructure.cache.ForgotPasswordCacheService;
import com.dishari.in.infrastructure.email.EmailService;
import com.dishari.in.utils.CookiesUtils;
import com.dishari.in.utils.IPUtils;
import com.dishari.in.utils.JwtUtils;
import com.dishari.in.web.dto.request.*;
import com.dishari.in.web.dto.response.LoginResponse;
import com.dishari.in.web.dto.response.MessageResponse;
import com.dishari.in.web.dto.response.RefreshTokenResponse;
import com.dishari.in.web.dto.response.UserResponse;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository ;
    private final PasswordEncoder passwordEncoder ;
    private final AuthenticationManager authenticationManager ;
    private final JwtUtils jwtUtils ;
    private final RefreshTokenRepository refreshTokenRepository ;
    private final CookiesUtils cookiesUtils ;
    private final EmailVerificationCacheService emailVerificationCacheService ;
    private final EmailService emailService ;
    private final ForgotPasswordCacheService forgotPasswordCacheService ;

    @Value("${app.pepper.password-pepper}")
    private String pepper ;

    @Override // Method for user registration
    @Transactional
    public MessageResponse userRegistration(UserRegistrationRequest request) {

        String email = request.getEmail() ;
        //First we check that the email should not be registered
        if (userRepository.existsByEmail(email)) {
            throw new EmailAlreadyExistException("Email is already registered.") ;
        }

        //Building user from request
        User user = buildUserFromRegistrationRequest(request) ;

        userRepository.save(user) ;

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                //We get the token from email verification cache service
                String token = emailVerificationCacheService.generateAndStoreToken(user.getId().toString());

                //Now send the mail
                emailService.sendVerificationEmail(user.getEmail(), user.getName(), token);
                return ;
            }
        });

        return buildMessageResponse(HttpStatus.OK , "Verification link has been sent to your email.") ;
    }

    @Override
    @Transactional
    public LoginResponse login(HttpServletRequest servletRequest, HttpServletResponse servletResponse, UserLoginRequest request) {
        //1. fetch the user by email
        User user = userRepository.findByEmailAndDeletedAtIsNull(request.getEmail())
                .orElseThrow(() -> new BadCredentialsException("Invalid Email or Password"));

        //2. check provider type (if social provider then user can't log in with password )
        if (user.getSocialProvider() != SocialProvider.LOCAL) {
            throw new SocialLoginRequiredException(
                    "This account uses " + user.getSocialProvider() + " login.");
        }

        //3. checking if the user is enabled or not
        if (!user.isEnabled()) {
            throw new EmailNotVerifiedException("Email is not verified") ;
        }
        if (user.isFrozen()) {
            throw new LockedException("Account is frozen. Contact support.");
        }
        if (!user.isVerified()) {
            throw new EmailNotVerifiedException("Email is not verified");
        }

        // Manually verify password — skip authenticationManager's DB lookup
        String pepperedPassword = pepperedPassword(request.getPassword()) ;
        if (!passwordEncoder.matches(pepperedPassword, user.getHashedPassword())) {
            throw new BadCredentialsException("Invalid Email or Password");
        }

        //5. Create an entry for refresh token
        String refreshTokenJti = UUID.randomUUID().toString() ;
        RefreshToken refreshTokenObj = buildRefreshToken(user , refreshTokenJti , servletRequest) ;
        refreshTokenRepository.save(refreshTokenObj) ;

        //6. generate refresh and access token
        String refreshToken = jwtUtils.generateRefreshToken(user , refreshTokenJti) ;
        String accessToken = jwtUtils.generateAccessToken(user) ;

        //7. set the refresh token to the HTTP only cookies
        cookiesUtils.attachRefreshTokenCookie(servletResponse , refreshToken);
        cookiesUtils.addNoStoreHeader(servletResponse);

        return  LoginResponse.fromEntity(accessToken , jwtUtils.getJwtAccessTokenExpirationInMS(),  user) ;
    }

    @Override
    @Transactional
    public RefreshTokenResponse refresh(HttpServletRequest servletRequest, HttpServletResponse servletResponse) {
        //1. fetch the refresh token from cookies
        Optional<String> refreshTokenOptional = cookiesUtils.extractRefreshTokenFromCookiesOrRequestBody(servletRequest) ;

        //2. check if the refresh token is present
        if (refreshTokenOptional.isEmpty()) {
            throw new RefreshTokenNotFoundException("Refresh token is not present");
        }

        String cookieRefreshToken = refreshTokenOptional.get() ;

        //Now check the token type
        if (!jwtUtils.isRefreshToken(cookieRefreshToken)) {
            throw new RefreshTokenNotFoundException("Invalid refresh token.") ;
        }

        String tokenId = jwtUtils.extractJti(cookieRefreshToken) ;
        RefreshToken refreshTokenObj = refreshTokenRepository.findByTokenId(tokenId)
                .orElseThrow(() -> new RefreshTokenNotFoundException("Invalid refresh token."));
        User tokenUser = refreshTokenObj.getUser() ;

        //The permission to use this token is revoked or not
        //TODO: Reuse Detection (The "Nuclear" Option)
        if (refreshTokenObj.isRevoked()) {
            revokeAllRefreshToken(tokenUser) ;
            cookiesUtils.clearRefreshTokenCookieFromHeader(servletResponse);

            //Logging the event
            log.warn("Reuse Detection : User ID : {} , IP Address : {}", tokenUser.getId() , IPUtils.extractIpAddress(servletRequest) );
            throw new RefreshTokenNotFoundException("Security Alert: This session has been invalidated.") ;
        }

        //Checking the expiration of the token
        if (refreshTokenObj.getExpiresAt().isBefore(Instant.now())) {
            refreshTokenObj.setRevoked(true);
            refreshTokenRepository.save(refreshTokenObj);
            cookiesUtils.clearRefreshTokenCookieFromHeader(servletResponse);
            throw new RefreshTokenNotFoundException("Refresh token expired.");
        }

        //TODO: IP county matching for security

        //Now, we rotate the refresh token
        String newJti = UUID.randomUUID().toString() ;
        refreshTokenObj.setRevoked(true);
        refreshTokenObj.setRotateToTokenId(newJti);

        //Generate new Access and Refresh Token
        String newAccessToken = jwtUtils.generateAccessToken(tokenUser) ;
        String newRefreshToken = jwtUtils.generateRefreshToken(tokenUser , newJti) ;

        //Extract user from refresh token object and build new refresh token entry and save it into the DB
        User refreshTokenUser = refreshTokenObj.getUser() ;
        RefreshToken newRefreshTokenObj = buildRefreshToken(refreshTokenUser , newJti , servletRequest) ;
        refreshTokenRepository.save(newRefreshTokenObj) ;

        //Add refresh token to the cookies
        cookiesUtils.attachRefreshTokenCookie(servletResponse , newRefreshToken);
        cookiesUtils.addNoStoreHeader(servletResponse);

        return RefreshTokenResponse.builder()
                .accessToken(newAccessToken)
                .accessTokenExpiration(jwtUtils.getJwtAccessTokenExpirationInMS())
                .build();
    }

    @Override
    @Transactional
    public MessageResponse logout(HttpServletRequest servletRequest, HttpServletResponse servletResponse) {
        cookiesUtils.extractRefreshTokenFromCookiesOrRequestBody(servletRequest).ifPresent(token -> {
            try {
                if (jwtUtils.isRefreshToken(token)){
                    String jti = jwtUtils.extractJti(token) ;
                    cookiesUtils.clearRefreshTokenCookieFromHeader(servletResponse);
                    refreshTokenRepository.findByTokenId(jti)
                            .ifPresent(refreshToken -> {
                                refreshToken.setRevoked(true);
                                refreshTokenRepository.save(refreshToken);
                            });
                }
            } catch(JwtException ignore) {

            }
        });
        clearSecurityContext(servletRequest , servletResponse) ;
        return buildMessageResponse(HttpStatus.OK , "Logout successfully.") ;
    }

    @Override
    @Transactional
    public MessageResponse logoutAll(HttpServletRequest servletRequest, HttpServletResponse servletResponse) {
        Optional<String> refreshTokenOptional = cookiesUtils.extractRefreshTokenFromCookiesOrRequestBody(servletRequest);

        if (refreshTokenOptional.isPresent()) {
            String token = refreshTokenOptional.get() ;
            try {
                if (jwtUtils.isRefreshToken(token)){
                    String jti = jwtUtils.extractJti(token) ;
                    cookiesUtils.clearRefreshTokenCookieFromHeader(servletResponse);
                    refreshTokenRepository.findByTokenId(jti)
                            .ifPresent(refreshToken -> {
                                User tokenUser = refreshToken.getUser() ;
                                revokeAllRefreshToken(tokenUser) ;
                            });
                }
            } catch(JwtException ignore) {

            }
        }else {
            logoutUsingAccessToken(servletRequest , servletResponse );
        }

        clearSecurityContext(servletRequest , servletResponse) ;
        return buildMessageResponse(HttpStatus.OK , "All Session logged out successfully.") ;
    }

    @Override
    @Transactional
    public MessageResponse verifyEmail(String token) {
        // 1. Consume immediately: This is an atomic "get-and-delete" in Redis
        String userId = emailVerificationCacheService.validateAndConsume(token)
                .orElseThrow(() -> new InvalidVerificationToken("Token expired or invalid."));

        // 2. Fetch User
        User user = userRepository.findById(UUID.fromString(userId))
                .orElseThrow(() -> new UserNotFoundException("User associated with token not found."));

        // 3. Update Status
        user.setEnabled(true);
        user.setVerified(true);
        user.setStatus(UserStatus.ACTIVE);
        userRepository.save(user);

        return buildMessageResponse(HttpStatus.OK , "Email verified successfully.") ;
    }

    @Override
    @Transactional
    public MessageResponse forgotPassword(HttpServletRequest servletRequest, HttpServletResponse servletResponse, UserForgotPasswordRequest request) {

        String email = request.getEmail() ;

        if (!forgotPasswordCacheService.canResendVerification(email)) {
            //Set try after header
            servletResponse.setHeader("Retry-After", Instant.now().plusMillis(15).toString());
            throw new TooManyRequestException("Too many request. Try after sometime.") ;
        }

        Optional<User> userOpt = userRepository.findByEmailAndDeletedAtIsNull(email);

        if (userOpt.isEmpty()) {
            // Silent return — don't reveal email doesn't exist
            return buildMessageResponse(HttpStatus.OK , "If this email is registered, a reset link has been sent.") ;
        }

        User user = userOpt.get() ;

        //Verify that the user must be logged in without any social provider
        if (user.getSocialProvider() != SocialProvider.LOCAL) {
            throw new ExternalAuthenticationException("Password change failed. Your account was created using "+ user.getSocialProvider().toString() +". Please manage your security settings directly through your social login provider.") ;
        }

        //Now we store the forgot password token into redis with TTL = 15 min
        String token = forgotPasswordCacheService.generateAndStoreToken(user.getId().toString());

        //Send the email
        emailService.sendForgotPasswordEmail(email, user.getName() , token);

        return buildMessageResponse(HttpStatus.OK , "Forgot password link sent successfully to your email");
    }

    @Override
    @Transactional
    public MessageResponse resetPassword(UserResetPasswordRequest request) {

        String token = request.getToken() ;

        String userId = forgotPasswordCacheService.validateAndConsume(token).orElseThrow(() -> new InvalidVerificationToken("Token expired or invalid.")) ;

        User tokenUser = userRepository.findById(UUID.fromString(userId)).orElseThrow(() -> new UserNotFoundException("User associated with token not found.")) ;

        String rawPassword =  request.getPassword() ;

        String pepperedPassword = pepperedPassword(rawPassword ) ;

        tokenUser.setHashedPassword(passwordEncoder.encode(pepperedPassword)) ;
        userRepository.save(tokenUser) ;

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                //Send email that password has been changed
                emailService.sendPasswordChangedEmail(tokenUser.getEmail() , tokenUser.getName()) ;
                return ;
            }
        });

        return buildMessageResponse(HttpStatus.OK , "Password reset successfully.") ;
    }

    @Override
    @Transactional
    public MessageResponse resendVerification(UserResendVerificationRequest request) {
        //First we check that the rate limit is not exceeded
        String email = request.getEmail() ;

        //Rate limit
        if (!emailVerificationCacheService.canResendVerification(email)) {
            //Set try after header
            throw new TooManyRequestException("Too many request. Try after sometime.") ;
        }

        Optional<User> userOpt = userRepository.findByEmailAndDeletedAtIsNull(email) ;

        if (userOpt.isEmpty()){
            return buildMessageResponse(HttpStatus.OK , "If this email is registered, a verification link has been sent.") ;
        }

        User user = userOpt.get() ;

        //Checking if the user is already verified then return
        if (user.isVerified()) {
            return buildMessageResponse(HttpStatus.OK , "Email is already verified.") ;
        }

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                //Now we store the verification token into redis with TTL = 15 min
                String token = emailVerificationCacheService.generateAndStoreToken(user.getId().toString());
                //Send email that verification link has been sent
                emailService.sendVerificationEmail(email , user.getName() , token) ;
            }
        });

        return buildMessageResponse(HttpStatus.OK , "Verification link sent successfully to your email.") ;
    }

    @Override
    public UserResponse getUser(String email) {
        User user = userRepository.findByEmailAndDeletedAtIsNull(email).orElseThrow(() -> new UserNotFoundException("User not found.")) ;

        return UserResponse.fromEntity(user);
    }

    @Override
    @Transactional
    public UserResponse updateUser(String email, UserUpdateRequest request) {
        Optional<User> userOpt = userRepository.findByEmailAndDeletedAtIsNull(email) ;
        if (userOpt.isEmpty()) {
            throw new UserNotFoundException("User not found.") ;
        }
        User user = userOpt.get() ;

        String name = user.getName() ;

        if (request.getName() != null && !request.getName().trim().isEmpty()) {
            name = request.getName() ;
        }

        String timeZone = user.getTimeZone() ;

        if (request.getTimeZone() != null && !request.getTimeZone().trim().isEmpty()) {
            timeZone = request.getTimeZone() ;
        }

        String avatarUrl = user.getAvatarUrl() ;

        if (request.getAvatarUrl() != null && !request.getAvatarUrl().trim().isEmpty()) {
            avatarUrl = request.getAvatarUrl() ;
        }

        user.setName(name) ;
        user.setTimeZone(timeZone) ;
        user.setAvatarUrl(avatarUrl) ;
        userRepository.save(user) ;

        return UserResponse.fromEntity(user) ;
    }


    //Helper methods ---> To revoke all the refresh token and end all the session if reuse detected
    private void revokeAllRefreshToken(User user) {
        refreshTokenRepository.revokeAllTokensByUser(user.getId()) ;
    }

    //Helper Method ---> To clear the security context and cookies
    private void clearSecurityContext(HttpServletRequest request , HttpServletResponse response) {
        SecurityContextHolder.clearContext();
        cookiesUtils.clearRefreshTokenCookieFromHeader(response);
        cookiesUtils.addNoStoreHeader(response);
    }

    private void logoutUsingAccessToken(HttpServletRequest request , HttpServletResponse response) {
        String accessToken = jwtUtils.extractTokenFromHeader(request) ;
        String userId = jwtUtils.extractUserIdFromAccessToken(accessToken);
        UUID uuidUserId = UUID.fromString(userId) ;
        Optional<User> user = userRepository.findById(uuidUserId) ;

        if (user.isEmpty()) {
            return ;
        }else {
            revokeAllRefreshToken(user.get());
        }
    }

    //Helper method ---> To build the refresh token object
    private RefreshToken buildRefreshToken(User user , String refreshTokenJti , HttpServletRequest servletRequest) {
        String userAgent = IPUtils.extractUserAgent(servletRequest) ;
        String ipAddress = IPUtils.extractIpAddress(servletRequest) ;

        return RefreshToken.builder()
                .tokenId(refreshTokenJti)
                .user(user)
                .expiresAt(Instant.now().plusMillis(jwtUtils.getJwtRefreshTokenExpirationInMS()))
                .revoked(false)
                .userAgent(userAgent)
                .ipAddress(ipAddress)
                .build() ;
    }

    //Helper method ---> To build the user object from registration request
    private User buildUserFromRegistrationRequest(UserRegistrationRequest request) {
        String pepperedPassword = pepperedPassword(request.getPassword()) ;
        return User.builder()
                .email(request.getEmail())
                .hashedPassword(passwordEncoder.encode(pepperedPassword))
                .name(request.getName())
                .enabled(false)
                .verified(false)
                .frozen(false)
                .hasPremium(false)
                .planExpiry(null)
                .socialProvider(SocialProvider.LOCAL)
                .status(UserStatus.VERIFICATION_PENDING)
                .avatarUrl(request.getAvatarUrl())
                .build() ;
    }

    //Helper Method ---> To build the message response
    private MessageResponse buildMessageResponse(HttpStatus status , String message) {
        return MessageResponse.builder()
                .status(status.value())
                .message(message)
                .timestamp(Instant.now())
                .build() ;
    }

    private String pepperedPassword(String rawPassword) {
        return rawPassword + pepper ;
    }
}
