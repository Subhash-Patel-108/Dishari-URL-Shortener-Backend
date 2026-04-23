package com.dishari.in.config;

import com.dishari.in.application.serviceImpl.CustomUserDetailService;
import com.dishari.in.filter.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final CustomUserDetailService customUserDetailService;
    private final ObjectMapper objectMapper;
//    private final AuthenticationSuccessHandler authenticationSuccessHandler;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // 1. Disable CSRF as it is not needed for stateless REST APIs using JWTs
                .csrf(AbstractHttpConfigurer::disable)

                // 2. Enable CORS with default settings (looks for a CorsConfigurationSource bean)
                .cors(Customizer.withDefaults())

                // 3. Configure URL-based authorization
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(AppConstants.AUTH_PUBLIC_URLS).permitAll()
                        .requestMatchers("/api/v1/user/all").hasRole("ADMIN")
                        .anyRequest().authenticated()
                )

                // Spring Security will not create or use HTTP Sessions
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // 5. Custom Error Handling for unauthorized requests
//                .exceptionHandling(ex -> ex.authenticationEntryPoint((request, response, exception) -> {
//                    response.setStatus(401);
//                    response.setContentType("application/json");
//
//                    Map<String, Object> map = new HashMap<>();
//                    map.put("timestamp", LocalDateTime.now().toString());
//                    map.put("status", 401);
//                    map.put("message", "Unauthorized access: " + exception.getMessage());
//                    response.getWriter().write(objectMapper.writeValueAsString(map));
//                }))

                // 6. Set the authentication provider logic
                .authenticationProvider(authenticationProvider())

                //7. OAuth2 Login Configuration
//                .oauth2Login(oauth2 -> oauth2
//                        .authorizationEndpoint(auth -> auth
//                                .authorizationRequestRepository(
//                                        new HttpSessionOAuth2AuthorizationRequestRepository()
//                                )
//                        )
//                        .failureHandler((request, response, exception) -> {
//                            response.setStatus(401);
//                            response.setContentType("application/json");
//                            Map<String, Object> map = new HashMap<>();
//                            map.put("timestamp", LocalDateTime.now().toString());
//                            map.put("status", 401);
//                            map.put("message", "OAuth2 login failed: " + exception.getMessage());
//                            response.getWriter().write(objectMapper.writeValueAsString(map));
//                        })
//                        .successHandler(authenticationSuccessHandler)
//                )
                .logout(AbstractHttpConfigurer::disable)
                // 8. Insert the JWT Filter before the standard UsernamePassword filter
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    //Dao Authentication provider
    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authenticationProvider = new DaoAuthenticationProvider(customUserDetailService) ;
        authenticationProvider.setPasswordEncoder(passwordEncoder());
        return authenticationProvider ;
    }
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }

    //Password encoder
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12) ;
    }
}
