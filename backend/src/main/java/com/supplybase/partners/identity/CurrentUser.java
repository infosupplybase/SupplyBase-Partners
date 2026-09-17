package com.supplybase.partners.identity;

import com.supplybase.partners.common.domain.ForbiddenException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/** Resolves the authenticated principal from the session -- never from a client-supplied id. */
@Component
public class CurrentUser {

    public AppUserPrincipal principal() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof AppUserPrincipal principal)) {
            throw new ForbiddenException("Not authenticated.");
        }
        return principal;
    }

    public Long userId() {
        return principal().getUserId();
    }

    public void requireRole(Role role) {
        if (!principal().hasRole(role)) {
            throw new ForbiddenException("This action requires the " + role + " role.");
        }
    }
}
