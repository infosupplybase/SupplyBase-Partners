package com.supplybase.partners.money;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "earning_entry")
@Getter
@Setter
@NoArgsConstructor
public class EarningEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "partner_id", nullable = false)
    private Long partnerId;

    @Column(name = "service_request_id", nullable = false, unique = true)
    private Long serviceRequestId;

    @Column(name = "gross_paise", nullable = false)
    private long grossPaise;

    @Column(name = "commission_paise", nullable = false)
    private long commissionPaise;

    @Column(name = "net_paise", nullable = false)
    private long netPaise;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private Status status = Status.EARNED;

    @Column(name = "earned_at", nullable = false)
    private Instant earnedAt;

    @Column(name = "available_at", nullable = false)
    private Instant availableAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) createdAt = Instant.now();
    }

    public enum Status { EARNED, REVERSED }
}
