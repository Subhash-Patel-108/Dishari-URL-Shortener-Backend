package com.dishari.in.exception;

public class InvalidVerificationToken extends RuntimeException{

    public InvalidVerificationToken(String message) {
        super(message);
    }

    public InvalidVerificationToken(){
        super("Verification token is invalid.") ;
    }
}
