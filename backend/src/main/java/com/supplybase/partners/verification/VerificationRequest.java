package com.supplybase.partners.verification;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "verification_request")
@Getter
@Setter
@NoArgsConstructor
public class VerificationRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "partner_id", nullable = false, unique = true)
    private Long partnerId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private Provider provider = Provider.MANUAL_REVIEW;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private Status status = Status.NOT_STARTED;

    @Column(name = "reviewer_user_id")
    private Long reviewerUserId;

    @Column(length = 500)
    private String reason;

    @Column(name = "submitted_at")
    private Instant submittedAt;

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

    public enum Provider { DEV_SIMULATOR, MANUAL_REVIEW }

    public enum Status { NOT_STARTED, SUBMITTED, PENDING_REVIEW, APPROVED, REJECTED }
}
