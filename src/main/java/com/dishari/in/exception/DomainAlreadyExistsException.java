package com.dishari.in.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class DomainAlreadyExistsException extends RuntimeException {
    public DomainAlreadyExistsException(String domain) {
        super("Domain '" + domain + "' is already registered");
    }
}
