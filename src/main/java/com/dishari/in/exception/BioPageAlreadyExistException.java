package com.dishari.in.exception;

public class BioPageAlreadyExistException extends RuntimeException{
    public BioPageAlreadyExistException(String message) {
        super(message) ;
    }

    public BioPageAlreadyExistException() {
        super("Bio page already exists");
    }
}
