package com.dishari.in.exception;

public class LockedException extends RuntimeException{
    public LockedException(String message) {
        super(message) ;
    }

    public LockedException() {
        super("Account is frozen. Contact support.") ;
    }
}
