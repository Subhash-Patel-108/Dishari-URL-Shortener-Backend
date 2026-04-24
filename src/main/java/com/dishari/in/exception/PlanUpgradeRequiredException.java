package com.dishari.in.exception;

public class PlanUpgradeRequiredException extends RuntimeException {
    public PlanUpgradeRequiredException(String message) {
        super(message);
    }

    public PlanUpgradeRequiredException() {
        super("Plan upgrade to access this service") ;
    }
}
