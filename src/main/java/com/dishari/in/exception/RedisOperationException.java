package com.dishari.in.exception;

public class RedisOperationException extends RuntimeException{
    public RedisOperationException(String message) {
        super(message) ;
    }

    public RedisOperationException() {
        super("Redis operation failed" ) ;
    }
}
