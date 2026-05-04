package com.dishari.in.exception;

public class InvalidSortFieldException extends RuntimeException{
    public InvalidSortFieldException(String message ) {
        super(message) ;
    }

    public InvalidSortFieldException() {
        super("Invalid sort field type.");
    }
}
