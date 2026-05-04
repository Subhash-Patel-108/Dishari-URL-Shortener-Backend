package com.dishari.in.utils;

import com.dishari.in.domain.entity.User;
import com.dishari.in.domain.enums.UserRole;
import com.dishari.in.exception.JwtAuthenticationException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;

@Component
@Getter
public class JwtUtils {

    private final String jwtSecret ;
    private final long jwtAccessTokenExpirationInMS ;
    private final long jwtRefreshTokenExpirationInMS ;
    private SecretKey key ;

    private static final String CLAIM_TYPE = "type";
    private static final String CLAIM_ROLE = "role";
    private static final String CLAIM_USER_ID = "userId";
    private static final String CLAIM_PLAN = "plan" ;
    private static final String CLAIM_HAS_PREMIUM = "hasPremium" ;
    private static final String CLAIM_PLAN_EXPIRY = "planExpiry" ;

    private static final String AUTH_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private static final String ACCESS = "accessToken";
    private static final String REFRESH = "refreshToken";

    public JwtUtils(
            @Value("${jwt.secret}") String jwtSecret ,
            @Value("${jwt.access-token-expiration-in-ms}") int jwtAccessTokenExpirationInMS ,
            @Value("${jwt.refresh-token-expiration-in-ms}") int jwtRefreshTokenExpirationInMS
    ) {
        if (jwtSecret.trim().length() < 64) {
            throw new JwtAuthenticationException("JWT secret must be at least 64 characters long");
        }

        this.jwtSecret = jwtSecret ;
        this.jwtAccessTokenExpirationInMS = jwtAccessTokenExpirationInMS ;
        this.jwtRefreshTokenExpirationInMS = jwtRefreshTokenExpirationInMS ;
    }

    @PostConstruct
    void init() {
        this.key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
    }


    //Method to extract token from http servlet request header
    public String extractTokenFromHeader(HttpServletRequest request) {
        String header = request.getHeader(AUTH_HEADER);

        if (header == null || !header.startsWith(BEARER_PREFIX)) {
            throw new JwtAuthenticationException("Missing or invalid Authorization header") ;
        }
        //The token is in the form of Bearer <token>
        return header.substring(BEARER_PREFIX.length()) ;
    }

    //Method to generate Access token from user data
    public String generateAccessToken(User user) {
        String role = user.getRole() == null ? UserRole.ROLE_USER.toString() : user.getRole().name() ;
        return Jwts.builder()
                .header().add("typ" , "JWT").and()
                .id(UUID.randomUUID().toString())
                .subject(user.getEmail())
                .claim(CLAIM_ROLE , role)
                .claim(CLAIM_TYPE , ACCESS)
                .claim(CLAIM_USER_ID , user.getId().toString())
                .claim(CLAIM_PLAN , user.getPlan().name())
                .claim(CLAIM_HAS_PREMIUM , user.isHasPremium())
                .claim(CLAIM_PLAN_EXPIRY , user.getPlanExpiry())
                .issuedAt(new Date())
                .expiration(new Date(new Date().getTime() + jwtAccessTokenExpirationInMS))
                .signWith(key)
                .compact();
    }

    //Method to create refresh token
    public String generateRefreshToken(User user , String tokenId) {
        return Jwts.builder()
                .header().add("typ" , "JWT").and()
                .id(tokenId)
                .subject(user.getEmail())
                .claim(CLAIM_TYPE , REFRESH)
                .claim(CLAIM_USER_ID , user.getId().toString())
                .issuedAt(new Date())
                .expiration(new Date(new Date().getTime() + jwtRefreshTokenExpirationInMS))
                .signWith(key)
                .compact();
    }

    //Method to check weather the given token is access token or not
    public void validateAccessToken(String token) {
        Claims claims = extractClaims(token) ;

        String type = claims.get(CLAIM_TYPE , String.class) ;

        if (!ACCESS.equals(type)) {
            throw new JwtAuthenticationException("Invalid access token") ;
        }
        return ;
    }

    //Method to check weather the given token is refresh token or not
    public void validateRefreshToken(String token) {
        Claims claims = extractClaims(token) ;

        String type = claims.get(CLAIM_TYPE , String.class) ;
        if (!REFRESH.equals(type)) {
            throw new JwtAuthenticationException("Invalid refresh token") ;
        }
        return ;
    }

    //Method to extract email from access token
    public String extractEmailFromAccessToken(String token) {
        Claims claims = extractClaims(token) ;
        return claims.getSubject() ;
    }

    //Method to extract email from refresh token
    public String extractEmailFromRefreshToken(String token) {
        Claims claims = extractClaims(token) ;

        return claims.getSubject() ;
    }

    //Method to extract tokenId from token
    public String extractJti(String token) {
        return extractClaims(token).getId() ;
    }

    //Method to check weather the given token is refresh token or not
    public boolean isRefreshToken(String token) {
        return REFRESH.equals(extractClaims(token).get(CLAIM_TYPE , String.class)) ;
    }

    public String extractUserIdFromAccessToken(String token ) {
        return extractClaims(token).get(CLAIM_USER_ID , String.class) ;
    }

    private Jws<Claims> getClaimsFromToken(String token){
        try {
            return Jwts.parser()
                    .verifyWith(key)
                    .clockSkewSeconds(30)
                    .build()
                    .parseSignedClaims(token) ;
        } catch (ExpiredJwtException ex) {
            throw new JwtAuthenticationException("Token has expired");
        } catch (Exception ex) {
            throw new JwtAuthenticationException("Invalid token");
        }
    }
    
    public Claims extractClaims(String token) {
        return getClaimsFromToken(token).getPayload() ;
    }
} 
