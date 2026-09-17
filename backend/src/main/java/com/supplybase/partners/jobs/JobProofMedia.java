package com.supplybase.partners.jobs;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "job_proof_media")
@Getter
@Setter
@NoArgsConstructor
public class JobProofMedia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "service_request_id", nullable = false)
    private Long serviceRequestId;

    @Column(name = "storage_key", nullable = false, length = 300)
    private String storageKey;

    @Column(name = "content_type", nullable = false, length = 100)
    private String contentType;

    @Column(name = "uploaded_by_partner_id", nullable = false)
    private Long uploadedByPartnerId;

    @Column(name = "uploaded_at", nullable = false, updatable = false)
    private Instant uploadedAt;

    @PrePersist
    void prePersist() {
        if (uploadedAt == null) uploadedAt = Instant.now();
    }
}
