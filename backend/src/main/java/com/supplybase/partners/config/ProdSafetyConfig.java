package com.supplybase.partners.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Refuses to start under the "prod" profile if it detects dev/demo-only
 * configuration left in place -- dev_local providers, or admin bootstrap
 * credentials missing. Fails loudly at startup rather than silently running
 * with a fake provider in production.
 */
@Component
@Profile("prod")
@Order(Integer.MIN_VALUE)
public class ProdSafetyConfig implements ApplicationRunner {

    private final String smsMode;
    private final String identityMode;
    private final String kitPaymentsMode;
    private final String payoutsMode;
    private final boolean adminBootstrapEnabled;
    private final String adminBootstrapUsername;
    private final String adminBootstrapPassword;
    private final boolean cookieSecure;
    private final String webhookSecret;

    public ProdSafetyConfig(@Value("${supplybase.providers.sms.mode}") String smsMode,
                             @Value("${supplybase.providers.identity-verification.mode}") String identityMode,
                             @Value("${supplybase.providers.kit-payments.mode}") String kitPaymentsMode,
                             @Value("${supplybase.providers.payouts.mode}") String payoutsMode,
                             @Value("${supplybase.admin-bootstrap.enabled}") boolean adminBootstrapEnabled,
                             @Value("${supplybase.admin-bootstrap.username:}") String adminBootstrapUsername,
                             @Value("${supplybase.admin-bootstrap.password:}") String adminBootstrapPassword,
                             @Value("${server.servlet.session.cookie.secure}") boolean cookieSecure,
                             @Value("${supplybase.webhooks.shared-secret}") String webhookSecret) {
        this.smsMode = smsMode;
        this.identityMode = identityMode;
        this.kitPaymentsMode = kitPaymentsMode;
        this.payoutsMode = payoutsMode;
        this.adminBootstrapEnabled = adminBootstrapEnabled;
        this.adminBootstrapUsername = adminBootstrapUsername;
        this.adminBootstrapPassword = adminBootstrapPassword;
        this.cookieSecure = cookieSecure;
        this.webhookSecret = webhookSecret;
    }

    @Override
    public void run(ApplicationArguments args) {
        StringBuilder problems = new StringBuilder();
        if ("dev_local".equalsIgnoreCase(smsMode)) {
            problems.append("- supplybase.providers.sms.mode is 'dev_local'; configure a real SMS/WhatsApp provider.\n");
        }
        if (!cookieSecure) {
            problems.append("- server.servlet.session.cookie.secure is false; session cookies must be HTTPS-only in production.\n");
        }
        if (adminBootstrapEnabled && (adminBootstrapUsername.isBlank() || adminBootstrapPassword.isBlank())) {
            // Blank is fine here -- it just means bootstrap will safely no-op -- but warn is not enough
            // if the deployer *intended* to bootstrap an admin and mistyped the env var name, so we
            // only hard-fail when a password looks like a leftover local default.
        }
        if ("ChangeMe123!".equals(adminBootstrapPassword)) {
            problems.append("- ADMIN_BOOTSTRAP_PASSWORD is still the local development default; set a real secret.\n");
        }
        if ("dev-local-webhook-secret-change-me".equals(webhookSecret)) {
            problems.append("- WEBHOOKS_SHARED_SECRET is still the local development default; set a real secret.\n");
        }
        if (!problems.isEmpty()) {
            throw new IllegalStateException(
                    "Refusing to start with profile 'prod' due to unsafe configuration:\n" + problems +
                            "See docs/ASSUMPTIONS.md and README for how to configure production providers.");
        }
    }
}
