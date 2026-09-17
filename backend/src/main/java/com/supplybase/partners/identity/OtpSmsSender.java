package com.supplybase.partners.identity;

import com.supplybase.partners.common.domain.ProviderUnavailableException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Delivers OTP codes. In "dev_local" mode nothing is actually sent -- the code
 * is logged and kept in memory for retrieval via a development-only endpoint,
 * and the app must never claim an SMS was sent. Any other mode requires a real
 * provider integration that does not exist yet in this build; rather than
 * silently no-op, it fails clearly so a misconfigured production deployment is
 * never mistaken for a working one.
 */
@Component
public class OtpSmsSender {

    private static final Logger log = LoggerFactory.getLogger(OtpSmsSender.class);

    private final String mode;
    private final Map<String, String> lastCodeByPhone = new ConcurrentHashMap<>();

    public OtpSmsSender(@Value("${supplybase.providers.sms.mode}") String mode) {
        this.mode = mode;
    }

    public OtpChallenge.OtpChannel send(String phoneE164, String code) {
        if ("dev_local".equalsIgnoreCase(mode)) {
            lastCodeByPhone.put(phoneE164, code);
            log.info("[DEV_LOCAL SMS] OTP for {} is {} (not actually sent; local development only)", phoneE164, code);
            return OtpChallenge.OtpChannel.DEV_LOCAL;
        }
        throw new ProviderUnavailableException(
                "SMS provider mode '" + mode + "' has no configured adapter in this deployment. " +
                        "Set supplybase.providers.sms.mode=dev_local for local/demo use, or wire a real SMS/WhatsApp provider.");
    }

    /** Development-only inspection of the last code sent to a phone; never used in production. */
    public String lastCodeFor(String phoneE164) {
        return lastCodeByPhone.get(phoneE164);
    }

    public boolean isDevLocal() {
        return "dev_local".equalsIgnoreCase(mode);
    }
}
