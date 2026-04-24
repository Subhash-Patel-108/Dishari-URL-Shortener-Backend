package com.dishari.in.aspect;

import com.dishari.in.annotation.RequiresPlan;
import com.dishari.in.domain.entity.User;
import com.dishari.in.domain.enums.Plan;
import com.dishari.in.domain.enums.UserRole;
import com.dishari.in.exception.PlanUpgradeRequiredException;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

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

        // ADMIN bypasses all plan restrictions
        if (user.getRole() == UserRole.ROLE_ADMIN) {
            return joinPoint.proceed();
        }

        Plan userPlan      = user.getPlan();
        List<Plan> allowed = Arrays.asList(requiresPlan.value());
        String feature     = requiresPlan.feature();

        if (!allowed.contains(userPlan)) {
            log.warn(
                    "Plan enforcement blocked: userId={} plan={} feature={}",
                    user.getId(), userPlan, feature
            );
            throw new PlanUpgradeRequiredException(
                    feature + " is not available on your current plan (" + userPlan + "). " +
                            "Required: " + allowed
            );
        }

        return joinPoint.proceed();
    }
}