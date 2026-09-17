package com.supplybase.partners.identity;

import com.supplybase.partners.common.domain.BadRequestException;
import com.supplybase.partners.common.domain.PhoneNumbers;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/** Development-only OTP inspection so a local/demo frontend can log in without real SMS. Never active in production. */
@RestController
@RequestMapping("/api/v1/auth/otp/dev")
@Profile("!prod")
public class AuthDevController {

    private final OtpSmsSender otpSmsSender;

    public AuthDevController(OtpSmsSender otpSmsSender) {
        this.otpSmsSender = otpSmsSender;
    }

    @GetMapping("/last-code")
    public ResponseEntity<Map<String, String>> lastCode(@RequestParam String phone) {
        if (!otpSmsSender.isDevLocal()) {
            throw new BadRequestException("SMS provider is not in dev_local mode; no code is available here.");
        }
        String normalized = PhoneNumbers.normalizeIndian(phone);
        String code = otpSmsSender.lastCodeFor(normalized);
        if (code == null) {
            throw new BadRequestException("No OTP has been requested yet for " + normalized);
        }
        return ResponseEntity.ok(Map.of("phone", normalized, "code", code));
    }
}
