package com.dishari.in.exception;

public class ExternalAuthenticationException extends RuntimeException{
    public ExternalAuthenticationException(String message) {
        super(message) ;
    }

    public ExternalAuthenticationException() {
        super("Social provider exception") ;
    }
}
