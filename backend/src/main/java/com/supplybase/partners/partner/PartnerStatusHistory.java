package com.supplybase.partners.partner;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "partner_status_history")
@Getter
@Setter
@NoArgsConstructor
public class PartnerStatusHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "partner_id", nullable = false)
    private Long partnerId;

    @Column(name = "from_status", length = 24)
    private String fromStatus;

    @Column(name = "to_status", nullable = false, length = 24)
    private String toStatus;

    @Column(length = 500)
    private String reason;

    @Column(name = "changed_by_user_id")
    private Long changedByUserId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) createdAt = Instant.now();
    }

    public PartnerStatusHistory(Long partnerId, String fromStatus, String toStatus, String reason, Long changedByUserId) {
        this.partnerId = partnerId;
        this.fromStatus = fromStatus;
        this.toStatus = toStatus;
        this.reason = reason;
        this.changedByUserId = changedByUserId;
    }
}
