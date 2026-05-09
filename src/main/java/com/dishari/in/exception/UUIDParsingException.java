package com.dishari.in.exception;

public class UUIDParsingException extends RuntimeException{
    public UUIDParsingException(String message) {
        super(message) ;
    }
    public UUIDParsingException() {
        super("Invalid UUID. Failed to parse UUID.") ;
    }
}
