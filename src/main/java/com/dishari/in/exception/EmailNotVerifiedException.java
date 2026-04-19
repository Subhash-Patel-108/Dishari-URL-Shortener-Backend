package com.dishari.in.exception;

public class EmailNotVerifiedException extends RuntimeException{
    public EmailNotVerifiedException(String message) {
        super(message) ;
    }

    public EmailNotVerifiedException() {
        super("Please verify your email first.") ;
    }
}
