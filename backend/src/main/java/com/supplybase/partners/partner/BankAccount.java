package com.supplybase.partners.partner;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/** Only a masked value and a hash live here -- see BankAccountService for masking/hash logic. */
@Entity
@Table(name = "bank_account")
@Getter
@Setter
@NoArgsConstructor
public class BankAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "partner_id", nullable = false, unique = true)
    private Long partnerId;

    @Column(name = "account_holder_name", nullable = false, length = 120)
    private String accountHolderName;

    @Column(name = "account_number_masked", nullable = false, length = 40)
    private String accountNumberMasked;

    @Column(name = "account_number_hash", nullable = false)
    private String accountNumberHash;

    @Column(nullable = false, length = 11)
    private String ifsc;

    @Column(name = "bank_name", length = 120)
    private String bankName;

    @Enumerated(EnumType.STRING)
    @Column(name = "verification_status", nullable = false, length = 24)
    private VerificationStatus verificationStatus = VerificationStatus.UNVERIFIED;

    @Column(name = "verified_at")
    private Instant verifiedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void prePersist() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = Instant.now();
    }

    public enum VerificationStatus { UNVERIFIED, FORMAT_VALID, PROVIDER_VERIFIED, FAILED }
}
