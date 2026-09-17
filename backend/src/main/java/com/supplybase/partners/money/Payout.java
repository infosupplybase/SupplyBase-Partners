package com.supplybase.partners.money;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "payout")
@Getter
@Setter
@NoArgsConstructor
public class Payout {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "partner_id", nullable = false)
    private Long partnerId;

    @Column(name = "payout_batch_id")
    private Long payoutBatchId;

    @Column(name = "bank_account_id", nullable = false)
    private Long bankAccountId;

    @Column(name = "amount_paise", nullable = false)
    private long amountPaise;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private Status status = Status.REQUESTED;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private Provider provider = Provider.DEV_SIMULATOR;

    @Column(name = "provider_reference", length = 120)
    private String providerReference;

    @Column(name = "failure_reason", length = 500)
    private String failureReason;

    @Column(name = "requested_at", nullable = false)
    private Instant requestedAt;

    @Column(name = "paid_at")
    private Instant paidAt;

    @PrePersist
    void prePersist() {
        if (requestedAt == null) requestedAt = Instant.now();
    }

    public enum Status { REQUESTED, SCHEDULED, PROCESSING, PAID, FAILED, MANUAL_RECONCILED }

    public enum Provider { DEV_SIMULATOR, MANUAL }
}
