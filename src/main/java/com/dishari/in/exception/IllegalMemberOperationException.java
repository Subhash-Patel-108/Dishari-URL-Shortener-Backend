package com.dishari.in.exception;

public class IllegalMemberOperationException extends RuntimeException{
    public IllegalMemberOperationException(String message) {
        super(message) ;
    }

    public IllegalMemberOperationException() {
        super("Member have no permission to perform this operation.") ;
    }
}
