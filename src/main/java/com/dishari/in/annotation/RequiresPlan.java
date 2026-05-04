package com.dishari.in.annotation;

import com.dishari.in.domain.enums.Plan;
import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequiresPlan {

    Plan[] value();

    String feature() default "This feature";

    // ── New flag — should we check plan expiry? ──────────────────
    // true  = check hasPremium + planExpiredAt (for paid features)
    // false = just check plan enum (for admin/role based)
    boolean checkExpiry() default true;
}