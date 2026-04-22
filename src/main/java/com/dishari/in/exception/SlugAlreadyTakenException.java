package com.dishari.in.exception;

public class SlugAlreadyTakenException extends RuntimeException{
    public SlugAlreadyTakenException(String message) {
        super(message);
    }
}
