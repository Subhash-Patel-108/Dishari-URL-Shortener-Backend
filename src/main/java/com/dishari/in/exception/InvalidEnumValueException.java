package com.dishari.in.exception;

public class InvalidEnumValueException extends RuntimeException{
    public InvalidEnumValueException(String message) {
        super(message) ;
    }

    public InvalidEnumValueException() {
        super("Invalid Enum value.") ;
    }
}
