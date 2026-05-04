package com.dishari.in.exception;

public class UserNotOwnException extends RuntimeException{

    public UserNotOwnException(String message) {
        super(message);
    }

    public UserNotOwnException() {
        super("The resource is not owned by you.") ;
    }
}
