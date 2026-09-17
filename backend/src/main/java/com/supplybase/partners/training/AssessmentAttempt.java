package com.supplybase.partners.training;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "assessment_attempt")
@Getter
@Setter
@NoArgsConstructor
public class AssessmentAttempt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "assessment_id", nullable = false)
    private Long assessmentId;

    @Column(name = "partner_id", nullable = false)
    private Long partnerId;

    @Column(name = "attempt_number", nullable = false)
    private int attemptNumber;

    @Column(name = "score_percent", nullable = false)
    private int scorePercent;

    @Column(nullable = false)
    private boolean passed;

    @Column(name = "submitted_at", nullable = false)
    private Instant submittedAt;

    @PrePersist
    void prePersist() {
        if (submittedAt == null) submittedAt = Instant.now();
    }
}
