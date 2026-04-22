package com.dishari.in.exception;

public class CustomSlugAlreadyExistsException extends RuntimeException{
    public CustomSlugAlreadyExistsException(String message) {
        super(message);
    }

    public CustomSlugAlreadyExistsException() {
        super("Custom slug already exists.");
    }

}
