package com.supplybase.partners.identity;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AppUserRepository extends JpaRepository<AppUser, Long> {
    Optional<AppUser> findByPhoneE164(String phoneE164);

    Optional<AppUser> findByUsername(String username);
}
