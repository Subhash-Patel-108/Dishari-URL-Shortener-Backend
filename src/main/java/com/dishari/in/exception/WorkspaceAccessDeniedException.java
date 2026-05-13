package com.dishari.in.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.FORBIDDEN)
public class WorkspaceAccessDeniedException extends RuntimeException {
    public WorkspaceAccessDeniedException(String message) {
        super(message);
    }

    public WorkspaceAccessDeniedException(String username, String operation , String workspaceSlug) {
        super("User " + username + " does not have access to " + operation +" workspace " + workspaceSlug);
    }
    public WorkspaceAccessDeniedException(String username , String workspaceSlug) {
        super("User " + username + " does not have access to workspace " + workspaceSlug);
    }
}