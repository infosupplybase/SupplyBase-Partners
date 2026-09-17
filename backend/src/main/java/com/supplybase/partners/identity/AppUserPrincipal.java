package com.supplybase.partners.identity;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

/** The authenticated principal stored in the session for both OTP (partner) and password (staff) logins. */
public class AppUserPrincipal implements UserDetails {

    private final Long userId;
    private final String loginId;
    private final String passwordHash;
    private final Set<Role> roles;
    private final boolean enabled;

    public AppUserPrincipal(AppUser user) {
        this.userId = user.getId();
        this.loginId = user.getUsername() != null ? user.getUsername() : user.getPhoneE164();
        this.passwordHash = user.getPasswordHash();
        this.roles = Set.copyOf(user.getRoles());
        this.enabled = user.getStatus() == AppUser.UserStatus.ACTIVE;
    }

    public Long getUserId() {
        return userId;
    }

    public Set<Role> getRoles() {
        return roles;
    }

    public boolean hasRole(Role role) {
        return roles.contains(role);
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return roles.stream().map(r -> new SimpleGrantedAuthority("ROLE_" + r.name())).collect(Collectors.toSet());
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public String getUsername() {
        return loginId;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }
}
