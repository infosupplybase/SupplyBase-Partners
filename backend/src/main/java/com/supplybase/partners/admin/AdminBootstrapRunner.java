package com.supplybase.partners.admin;

import com.supplybase.partners.identity.AppUser;
import com.supplybase.partners.identity.AppUserRepository;
import com.supplybase.partners.identity.Role;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Creates the first ADMIN account from environment variables on startup, if
 * none exists yet -- never from seed SQL, so no password ever sits in a
 * checked-in file. Safe to leave enabled: it is a no-op once any ADMIN exists.
 */
@Component
public class AdminBootstrapRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminBootstrapRunner.class);

    private final AppUserRepository appUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final boolean enabled;
    private final String username;
    private final String password;
    private final String phone;

    public AdminBootstrapRunner(AppUserRepository appUserRepository, PasswordEncoder passwordEncoder,
                                 @Value("${supplybase.admin-bootstrap.enabled}") boolean enabled,
                                 @Value("${supplybase.admin-bootstrap.username}") String username,
                                 @Value("${supplybase.admin-bootstrap.password}") String password,
                                 @Value("${supplybase.admin-bootstrap.phone}") String phone) {
        this.appUserRepository = appUserRepository;
        this.passwordEncoder = passwordEncoder;
        this.enabled = enabled;
        this.username = username;
        this.password = password;
        this.phone = phone;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!enabled) {
            return;
        }
        boolean anyAdmin = appUserRepository.findAll().stream().anyMatch(u -> u.getRoles().contains(Role.ADMIN));
        if (anyAdmin) {
            return;
        }
        if (username == null || username.isBlank() || password == null || password.isBlank()) {
            log.warn("No ADMIN account exists and ADMIN_BOOTSTRAP_USERNAME/ADMIN_BOOTSTRAP_PASSWORD are not set; " +
                    "skipping admin bootstrap. Set them and restart to create the first admin.");
            return;
        }
        AppUser admin = new AppUser();
        admin.setUsername(username);
        admin.setPasswordHash(passwordEncoder.encode(password));
        admin.setPhoneE164(phone);
        admin.setName("Administrator");
        admin.getRoles().add(Role.ADMIN);
        appUserRepository.save(admin);
        log.warn("Bootstrapped first ADMIN account with username '{}'. Change its password immediately after first login.", username);
    }
}
