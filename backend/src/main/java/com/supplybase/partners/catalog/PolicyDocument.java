package com.supplybase.partners.catalog;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "policy_document")
@Getter
@Setter
@NoArgsConstructor
public class PolicyDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private PolicyType type;

    @Column(nullable = false, length = 32)
    private String version;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(name = "content_markdown", nullable = false, columnDefinition = "MEDIUMTEXT")
    private String contentMarkdown;

    @Column(name = "is_current", nullable = false)
    private boolean current;

    @Column(name = "published_at", nullable = false)
    private Instant publishedAt;

    @PrePersist
    void prePersist() {
        if (publishedAt == null) publishedAt = Instant.now();
    }

    public enum PolicyType { TERMS, PRIVACY, MARKETING_CONSENT }
}
