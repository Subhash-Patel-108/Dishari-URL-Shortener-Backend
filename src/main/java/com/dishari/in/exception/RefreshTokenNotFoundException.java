package com.dishari.in.exception;

public class RefreshTokenNotFoundException extends RuntimeException{
    public RefreshTokenNotFoundException(String message) {
        super(message) ;
    }

    public RefreshTokenNotFoundException() {
        super("Refresh token not found. Please Login again.") ;
    }
}
