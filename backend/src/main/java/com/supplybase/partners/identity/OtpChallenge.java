package com.supplybase.partners.identity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "otp_challenge")
@Getter
@Setter
@NoArgsConstructor
public class OtpChallenge {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "phone_e164", nullable = false, length = 16)
    private String phoneE164;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private OtpPurpose purpose = OtpPurpose.LOGIN_OR_SIGNUP;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private OtpChannel channel;

    @Column(name = "code_hash", nullable = false)
    private String codeHash;

    @Column(nullable = false, columnDefinition = "INT UNSIGNED")
    private int attempts = 0;

    @Column(name = "max_attempts", nullable = false, columnDefinition = "INT UNSIGNED")
    private int maxAttempts = 5;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "consumed_at")
    private Instant consumedAt;

    @Column(name = "resend_available_at", nullable = false)
    private Instant resendAvailableAt;

    @Column(name = "requester_ip", length = 64)
    private String requesterIp;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }

    public boolean isConsumed() {
        return consumedAt != null;
    }

    public boolean attemptsExhausted() {
        return attempts >= maxAttempts;
    }

    public enum OtpPurpose { LOGIN_OR_SIGNUP }

    public enum OtpChannel { SMS, WHATSAPP, DEV_LOCAL }
}
