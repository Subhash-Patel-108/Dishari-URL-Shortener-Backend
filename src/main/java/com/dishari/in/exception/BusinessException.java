package com.dishari.in.exception;

public class BusinessException extends RuntimeException{
    public BusinessException(String message) {
        super(message);
    }

    public BusinessException() {
        super("Business Exception");
    }
}
