package com.dishari.in.exception;

public class TooManyRequestException extends RuntimeException{
    public TooManyRequestException(String message) {
        super(message);
    }

    public TooManyRequestException(){
        super("Too Many Requests. ");
    }
}
