package com.supplybase.partners.identity;

import com.supplybase.partners.common.domain.BadRequestException;
import com.supplybase.partners.common.domain.PhoneNumbers;
import com.supplybase.partners.common.domain.RateLimitException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;

/**
 * Phone-OTP request/verify with rate limiting, resend cooldown, expiry, and a
 * bounded number of verify attempts per challenge. Codes are never persisted
 * or logged in plaintext outside the dev-local sender.
 */
@Service
public class OtpService {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final BCryptPasswordEncoder ENCODER = new BCryptPasswordEncoder();

    private final OtpChallengeRepository otpChallengeRepository;
    private final OtpSmsSender smsSender;
    private final OtpProperties properties;

    public OtpService(OtpChallengeRepository otpChallengeRepository, OtpSmsSender smsSender, OtpProperties properties) {
        this.otpChallengeRepository = otpChallengeRepository;
        this.smsSender = smsSender;
        this.properties = properties;
    }

    public record RequestResult(String phoneE164, Instant expiresAt, Instant resendAvailableAt) {}

    @Transactional
    public RequestResult requestOtp(String rawPhone, String requesterIp) {
        String phone = PhoneNumbers.normalizeIndian(rawPhone);
        Instant now = Instant.now();

        Instant hourAgo = now.minus(Duration.ofHours(1));
        long recentCount = otpChallengeRepository.findAllByPhoneE164AndCreatedAtAfter(phone, hourAgo).size();
        if (recentCount >= properties.getMaxRequestsPerPhonePerHour()) {
            throw new RateLimitException("Too many OTP requests for this number. Please try again later.");
        }

        otpChallengeRepository.findFirstByPhoneE164OrderByCreatedAtDesc(phone).ifPresent(last -> {
            if (!last.isConsumed() && now.isBefore(last.getResendAvailableAt())) {
                long waitSeconds = Duration.between(now, last.getResendAvailableAt()).getSeconds();
                throw new RateLimitException("Please wait " + waitSeconds + "s before requesting another code.");
            }
        });

        String code = generateCode();
        OtpChallenge challenge = new OtpChallenge();
        challenge.setPhoneE164(phone);
        challenge.setPurpose(OtpChallenge.OtpPurpose.LOGIN_OR_SIGNUP);
        challenge.setCodeHash(ENCODER.encode(code));
        challenge.setMaxAttempts(properties.getMaxAttempts());
        challenge.setExpiresAt(now.plusSeconds(properties.getTtlSeconds()));
        challenge.setResendAvailableAt(now.plusSeconds(properties.getResendCooldownSeconds()));
        challenge.setRequesterIp(requesterIp);

        OtpChallenge.OtpChannel channel = smsSender.send(phone, code);
        challenge.setChannel(channel);
        otpChallengeRepository.save(challenge);

        return new RequestResult(phone, challenge.getExpiresAt(), challenge.getResendAvailableAt());
    }

    @Transactional
    public String verifyOtp(String rawPhone, String code) {
        String phone = PhoneNumbers.normalizeIndian(rawPhone);
        OtpChallenge challenge = otpChallengeRepository.findFirstByPhoneE164OrderByCreatedAtDesc(phone)
                .orElseThrow(() -> new BadRequestException("No OTP was requested for this number."));

        if (challenge.isConsumed()) {
            throw new BadRequestException("This code has already been used. Request a new one.");
        }
        if (challenge.isExpired()) {
            throw new BadRequestException("This code has expired. Request a new one.");
        }
        if (challenge.attemptsExhausted()) {
            throw new BadRequestException("Too many incorrect attempts. Request a new code.");
        }
        if (!ENCODER.matches(code, challenge.getCodeHash())) {
            challenge.setAttempts(challenge.getAttempts() + 1);
            otpChallengeRepository.save(challenge);
            int remaining = challenge.getMaxAttempts() - challenge.getAttempts();
            throw new BadRequestException("Incorrect code. " + Math.max(remaining, 0) + " attempt(s) remaining.");
        }

        challenge.setConsumedAt(Instant.now());
        otpChallengeRepository.save(challenge);
        return phone;
    }

    private String generateCode() {
        int max = (int) Math.pow(10, properties.getLength());
        int value = RANDOM.nextInt(max);
        return String.format("%0" + properties.getLength() + "d", value);
    }
}
