package com.supplybase.partners.screening;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "screening_booking")
@Getter
@Setter
@NoArgsConstructor
public class ScreeningBooking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "slot_id", nullable = false)
    private Long slotId;

    @Column(name = "partner_id", nullable = false)
    private Long partnerId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private Status status = Status.BOOKED;

    @Column(name = "checked_in_at")
    private Instant checkedInAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private Outcome outcome = Outcome.PENDING;

    @Column(name = "outcome_reason", length = 500)
    private String outcomeReason;

    @Column(name = "decided_by_user_id")
    private Long decidedByUserId;

    @Column(name = "decided_at")
    private Instant decidedAt;

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

    public enum Status { BOOKED, RESCHEDULED, CANCELLED, CHECKED_IN, COMPLETED }

    public enum Outcome { PENDING, PASSED, FAILED }
}
