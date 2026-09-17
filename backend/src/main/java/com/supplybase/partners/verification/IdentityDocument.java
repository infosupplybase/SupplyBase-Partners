package com.supplybase.partners.verification;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "identity_document")
@Getter
@Setter
@NoArgsConstructor
public class IdentityDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "partner_id", nullable = false)
    private Long partnerId;

    @Enumerated(EnumType.STRING)
    @Column(name = "doc_type", nullable = false, length = 24)
    private DocType docType;

    @Column(name = "storage_key", nullable = false, length = 300)
    private String storageKey;

    @Column(name = "original_filename", nullable = false)
    private String originalFilename;

    @Column(name = "content_type", nullable = false, length = 100)
    private String contentType;

    @Column(name = "size_bytes", nullable = false)
    private long sizeBytes;

    @Column(name = "masked_reference", length = 64)
    private String maskedReference;

    @Column(name = "uploaded_at", nullable = false, updatable = false)
    private Instant uploadedAt;

    @PrePersist
    void prePersist() {
        if (uploadedAt == null) uploadedAt = Instant.now();
    }

    public enum DocType { AADHAAR, PAN, OTHER }
}
