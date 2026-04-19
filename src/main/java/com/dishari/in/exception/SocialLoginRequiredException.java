package com.dishari.in.exception;

public class SocialLoginRequiredException extends RuntimeException{
    public SocialLoginRequiredException(String message) {
        super(message) ;
    }

    public SocialLoginRequiredException() {
        super("This account is register by social provider.") ;
    }
}
