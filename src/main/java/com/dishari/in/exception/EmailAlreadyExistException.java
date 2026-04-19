package com.dishari.in.exception;

public class EmailAlreadyExistException extends RuntimeException{
    public EmailAlreadyExistException(String message) {
        super(message) ;
    }

    public EmailAlreadyExistException() {
        super("Email is already registered. ") ;
    }
}
