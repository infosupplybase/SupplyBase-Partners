package com.supplybase.partners.partner;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

/**
 * The partner's onboarding/profile record. onboardingStage and activationStatus
 * are backend-owned state machines -- see PartnerService -- and are never
 * accepted as client-writable fields on a generic "update profile" endpoint.
 */
@Entity
@Table(name = "partner")
@Getter
@Setter
@NoArgsConstructor
public class Partner {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, unique = true)
    private Long userId;

    @Column(name = "primary_category_id")
    private Long primaryCategoryId;

    @Column(name = "residence_city_id")
    private Long residenceCityId;

    @Column(name = "photo_storage_key", length = 300)
    private String photoStorageKey;

    @Column(name = "experience_years", columnDefinition = "TINYINT UNSIGNED")
    private Integer experienceYears;

    @Column(length = 1000)
    private String bio;

    @Enumerated(EnumType.STRING)
    @Column(name = "working_hours_choice", length = 8)
    private WorkingHoursChoice workingHoursChoice;

    @Enumerated(EnumType.STRING)
    @Column(name = "onboarding_stage", nullable = false, length = 32)
    private OnboardingStage onboardingStage = OnboardingStage.BASIC_DETAILS;

    @Enumerated(EnumType.STRING)
    @Column(name = "activation_status", nullable = false, length = 24)
    private ActivationStatus activationStatus = ActivationStatus.NOT_ACTIVE;

    @Column(name = "suspended_reason", length = 500)
    private String suspendedReason;

    @Column(name = "activated_at")
    private Instant activatedAt;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "partner_language", joinColumns = @JoinColumn(name = "partner_id"))
    @Column(name = "language_code")
    private Set<String> languages = new HashSet<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    private Long version;

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

    public enum ActivationStatus { NOT_ACTIVE, ACTIVE, SUSPENDED }
}
