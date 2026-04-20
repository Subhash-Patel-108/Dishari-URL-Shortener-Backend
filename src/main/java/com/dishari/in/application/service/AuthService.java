package com.dishari.in.application.service;


import com.dishari.in.web.dto.request.*;
import com.dishari.in.web.dto.response.LoginResponse;
import com.dishari.in.web.dto.response.MessageResponse;
import com.dishari.in.web.dto.response.RefreshTokenResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;

public interface AuthService {

    MessageResponse userRegistration(@Valid UserRegistrationRequest request);

    LoginResponse login(HttpServletRequest servletRequest, HttpServletResponse servletResponse, UserLoginRequest request);

    RefreshTokenResponse refresh(HttpServletRequest servletRequest, HttpServletResponse servletResponse);

    MessageResponse logout(HttpServletRequest servletRequest, HttpServletResponse servletResponse);

    MessageResponse logoutAll(HttpServletRequest servletRequest, HttpServletResponse servletResponse);

    MessageResponse verifyEmail(String token);

    MessageResponse forgotPassword(HttpServletRequest servletRequest , HttpServletResponse servletResponse , UserForgotPasswordRequest request);

    MessageResponse resetPassword(UserResetPasswordRequest request);

    MessageResponse resendVerification(UserResendVerificationRequest request);
}
