package com.dishari.in.aspect;

import com.dishari.in.annotation.RequiresPlan;
import com.dishari.in.domain.entity.User;
import com.dishari.in.domain.enums.Plan;
import com.dishari.in.domain.enums.UserRole;
import com.dishari.in.exception.PlanExpiredException;
import com.dishari.in.exception.PlanUpgradeRequiredException;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;

@Aspect
@Component
@Slf4j
public class PlanEnforcementAspect {

    @Around("@annotation(requiresPlan)")
    public Object enforcePlan(
            ProceedingJoinPoint joinPoint,
            RequiresPlan requiresPlan) throws Throwable {

        Authentication auth = SecurityContextHolder
                .getContext().getAuthentication();

        // Not authenticated — let Spring Security handle it
        if (auth == null || !(auth.getPrincipal() instanceof User user)) {
            return joinPoint.proceed();
        }

        // ── ADMIN bypasses everything ────────────────────────────
        if (user.getRole() == UserRole.ROLE_ADMIN) {
            return joinPoint.proceed();
        }

        List<Plan> allowedPlans = Arrays.asList(requiresPlan.value());
        String feature          = requiresPlan.feature();
        boolean checkExpiry     = requiresPlan.checkExpiry();

        // ── Step 1: Check if user's plan is in allowed list ──────
        if (!allowedPlans.contains(user.getPlan())) {
            log.warn(
                    "Plan check failed: userId={} plan={} feature={} required={}",
                    user.getId(), user.getPlan(), feature, allowedPlans
            );
            throw new PlanUpgradeRequiredException(
                    feature + " requires " + allowedPlans + " plan. " +
                            "Your current plan: " + user.getPlan() + ". " +
                            "Please upgrade to access this feature."
            );
        }

        // ── Step 2: Check hasPremium flag ────────────────────────
        if (checkExpiry && !user.isHasPremium()) {
            log.warn(
                    "Premium flag check failed: userId={} plan={} " +
                            "hasPremium=false feature={}",
                    user.getId(), user.getPlan(), feature
            );
            throw new PlanUpgradeRequiredException(
                    feature + " requires an active premium subscription. " +
                            "Your premium access is not active."
            );
        }

        // ── Step 3: Check plan expiry ────────────────────────────
        if (checkExpiry && isPlanExpired(user)) {
            log.warn(
                    "Plan expired: userId={} plan={} expiredAt={} feature={}",
                    user.getId(),
                    user.getPlan(),
                    user.getPlanExpiry(),
                    feature
            );
            throw new PlanExpiredException(
                    "Your " + user.getPlan() + " plan expired on " +
                            user.getPlanExpiry() + ". " +
                            "Please renew your subscription to use: " + feature
            );
        }

        return joinPoint.proceed();
    }

    // ── Plan is expired if planExpiredAt is set and in the past ──
    private boolean isPlanExpired(User user) {
        return user.getPlanExpiry() != null &&
                user.getPlanExpiry().isBefore(Instant.now());
    }
}