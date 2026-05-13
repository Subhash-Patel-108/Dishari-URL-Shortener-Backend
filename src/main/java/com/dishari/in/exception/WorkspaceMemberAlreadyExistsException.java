package com.dishari.in.exception;

public class WorkspaceMemberAlreadyExistsException extends RuntimeException{
    public WorkspaceMemberAlreadyExistsException(String message) {
        super(message);
    }

    public WorkspaceMemberAlreadyExistsException() {
        super("The member already in workspace.Verification may be pending.") ;
    }
}
