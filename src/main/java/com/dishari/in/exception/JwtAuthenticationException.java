package com.dishari.in.exception;

public class JwtAuthenticationException extends RuntimeException{

    public JwtAuthenticationException(String message) {
        super(message) ;
    }

    public JwtAuthenticationException() {
        super("Jwt Authentication Exception");
    }
}
