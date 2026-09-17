package com.supplybase.partners.partner;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "partner_consent")
@Getter
@Setter
@NoArgsConstructor
public class PartnerConsent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "partner_id", nullable = false)
    private Long partnerId;

    @Column(name = "policy_document_id", nullable = false)
    private Long policyDocumentId;

    @Column(nullable = false)
    private boolean accepted = true;

    @Column(name = "accepted_at", nullable = false)
    private Instant acceptedAt;

    @Column(name = "ip_address", length = 64)
    private String ipAddress;

    @PrePersist
    void prePersist() {
        if (acceptedAt == null) acceptedAt = Instant.now();
    }
}
