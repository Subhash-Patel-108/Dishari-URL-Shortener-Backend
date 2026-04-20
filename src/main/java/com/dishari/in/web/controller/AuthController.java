package com.dishari.in.web.controller;

import com.dishari.in.application.service.AuthService;
import com.dishari.in.web.dto.request.*;
import com.dishari.in.web.dto.response.LoginResponse;
import com.dishari.in.web.dto.response.MessageResponse;
import com.dishari.in.web.dto.response.RefreshTokenResponse;
import com.dishari.in.web.dto.response.UserResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService ;

    //Registration endpoint
    @PostMapping("/register")
    public ResponseEntity<MessageResponse> register(
            @Valid @RequestBody UserRegistrationRequest request
    ) {
        MessageResponse response = authService.userRegistration(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    //Login endpoint
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(
            HttpServletRequest servletRequest ,
            HttpServletResponse servletResponse ,
            @Valid @RequestBody UserLoginRequest request
    ) {
        LoginResponse response = authService.login(servletRequest , servletResponse , request) ;
        return ResponseEntity.ok(response) ;
    }

    //Refresh Token --> Generate access token and rotate the refresh token
    @PostMapping("/refresh")
    public ResponseEntity<RefreshTokenResponse> refresh(
            HttpServletRequest servletRequest ,
            HttpServletResponse servletResponse
    ) {
        RefreshTokenResponse response = authService.refresh(servletRequest , servletResponse) ;
        return ResponseEntity.ok(response) ;
    }

    //Logout --> Logout the current session
    @PostMapping("/logout")
    public ResponseEntity<MessageResponse> logout(
            HttpServletRequest servletRequest ,
            HttpServletResponse servletResponse
    ) {
        MessageResponse response = authService.logout(servletRequest , servletResponse) ;
        return ResponseEntity.status(HttpStatus.OK).body(response) ;
    }


    //Logout --> Logout all sessions
    @PostMapping("/logout-all")
    public ResponseEntity<MessageResponse> logoutAll(
            HttpServletRequest servletRequest ,
            HttpServletResponse servletResponse
    ) {
        MessageResponse response = authService.logoutAll(servletRequest , servletResponse) ;
        return ResponseEntity.status(HttpStatus.OK).body(response) ;
    }

    //Verify Email --> Send token then verify it
    @GetMapping("/verify-email")
    public ResponseEntity<MessageResponse> verifyEmail(
            @RequestParam("token") String token
    ) {
        MessageResponse response = authService.verifyEmail(token);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    //Forgot Password --> To send the request for forgot password
    @PostMapping("/forgot-password")
    public ResponseEntity<MessageResponse> forgotPassword(
            HttpServletRequest servletRequest ,
            HttpServletResponse servletResponse ,
            @RequestBody @Valid UserForgotPasswordRequest request
    ) {
        MessageResponse response = authService.forgotPassword(servletRequest , servletResponse , request) ;
        return ResponseEntity.status(HttpStatus.OK).body(response) ;
    }

    //Change Password --> To change the password with a token
    @PostMapping("/reset-password")
    public ResponseEntity<MessageResponse> resetPassword(
            @Valid @RequestBody UserResetPasswordRequest request
    ){
        MessageResponse response = authService.resetPassword(request) ;
        return ResponseEntity.status(HttpStatus.OK).body(response) ;
    }

    //Resend Email Verification --> To send Email to verify the account
    @PostMapping("/resend-verification")
    public ResponseEntity<MessageResponse> resendVerification(
            @Valid @RequestBody UserResendVerificationRequest request
    ) {
        MessageResponse response = authService.resendVerification(request) ;
        return ResponseEntity.status(HttpStatus.OK).body(response) ;
    }
}
