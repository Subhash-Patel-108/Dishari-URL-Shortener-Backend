package com.dishari.in.exception;

public class MemberVerificationNotPendingException extends RuntimeException{
    public MemberVerificationNotPendingException(String message ) {
        super(message) ;
    }

    public MemberVerificationNotPendingException() {
        super("Member already verified.") ;
    }
}
