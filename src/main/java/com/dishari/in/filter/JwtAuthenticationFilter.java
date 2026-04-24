package com.dishari.in.filter;

import com.dishari.in.config.AppConstants;
import com.dishari.in.domain.entity.User;
import com.dishari.in.domain.enums.Plan;
import com.dishari.in.domain.enums.UserRole;
import com.dishari.in.exception.JwtAuthenticationException;
import com.dishari.in.utils.JwtUtils;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtils jwtUtils ;
    // Kept for potential use in sensitive endpoints requiring fresh DB validation
    // private final CustomUserDetailService customUserDetailService ;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            // 1. Extract token from the Authorization header
            String token = jwtUtils.extractTokenFromHeader(request) ;

            if (token != null) {

                // 1. Single Parse: Get claims and validate in one crypto operation
                Claims claims = jwtUtils.extractClaims(token) ;

                // 2. Validate Type (Must be Access Token)
                if (!"accessToken".equals(claims.get("type" , String.class))) {
                    throw new JwtAuthenticationException("Invalid token type");
                }

                String email = claims.getSubject();
                String role = claims.get("role", String.class);
                String userId = claims.get("userId", String.class);
                String plan = claims.get("plan", String.class);

                if (email == null || userId == null) {
                    throw new JwtAuthenticationException("Missing email or user id");
                }

                if (SecurityContextHolder.getContext().getAuthentication() == null) {

                    // 3. ZERO-DB LOOKUP: Create a transient User object
                    // We don't call userRepository.findByEmail()!
                    User principal = User.builder()
                            .email(email)
                            .id(UUID.fromString(userId))
                            .role(UserRole.valueOf(role == null ? UserRole.ROLE_USER.name() : role))
                            .plan(Plan.valueOf(plan == null ? Plan.FREE.name() : plan))
                            .enabled(true) // We trust the token's existence means they were enabled
                            .build();

                    UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                            principal, null, principal.getAuthorities()
                    );

                    auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                    SecurityContext context = SecurityContextHolder.createEmptyContext();
                    context.setAuthentication(auth);
                    SecurityContextHolder.setContext(context);
                }
            }
        } catch (Exception ex) {
            // We don't want to throw an exception here because it stops the filter chain.
            // We let it pass; SecurityContext will be empty, and the entry point will handle the 401/403.
            log.warn("JWT Authentication failed: {}", ex.getClass().getSimpleName());
        }

        filterChain.doFilter(request , response);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        String path = request.getServletPath();
        // Skip the JWT filter for all public URLs defined in your constants
        for (String url : AppConstants.AUTH_PUBLIC_URLS) {
            // Handle wildcards like /**
            if (url.endsWith("/**")) {
                String base = url.substring(0, url.length() - 3);
                if (path.startsWith(base)) return true;
            } else if (url.equals(path)) {
                return true;
            }
        }
        return false;
    }
}
