package com.dishari.in.exception;

public class AccessDeniedException extends RuntimeException{
    public AccessDeniedException(String message) {
        super(message);
    }

    public AccessDeniedException() {
        super("Access Denied");
    }
}
