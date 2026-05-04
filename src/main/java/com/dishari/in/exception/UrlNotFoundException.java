package com.dishari.in.exception;

public class UrlNotFoundException extends RuntimeException{
    public UrlNotFoundException(String message) {
        super(message) ;
    }

    public UrlNotFoundException() {
        super("The Url not found with the given id.") ;
    }
}
