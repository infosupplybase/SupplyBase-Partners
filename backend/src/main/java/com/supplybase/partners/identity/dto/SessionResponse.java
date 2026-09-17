package com.supplybase.partners.identity.dto;

import com.supplybase.partners.identity.Role;

import java.util.Set;

public record SessionResponse(boolean authenticated, Long userId, String displayName, Set<Role> roles) {
    public static SessionResponse anonymous() {
        return new SessionResponse(false, null, null, Set.of());
    }
}
