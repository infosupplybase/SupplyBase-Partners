package com.supplybase.partners.jobs;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "service_request")
@Getter
@Setter
@NoArgsConstructor
public class ServiceRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "customer_id", nullable = false)
    private Long customerId;

    @Column(name = "service_id", nullable = false)
    private Long serviceId;

    @Column(name = "address_id", nullable = false)
    private Long addressId;

    @Column(name = "scheduled_at", nullable = false)
    private Instant scheduledAt;

    @Column(length = 1000)
    private String notes;

    @Column(name = "job_value_paise", nullable = false)
    private long jobValuePaise;

    @Column(name = "partner_earning_paise", nullable = false)
    private long partnerEarningPaise;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private Status status = Status.OPEN;

    @Column(name = "assigned_partner_id")
    private Long assignedPartnerId;

    @Column(name = "created_by_admin_id", nullable = false)
    private Long createdByAdminId;

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

    public enum Status { OPEN, ASSIGNED, IN_PROGRESS, COMPLETION_SUBMITTED, COMPLETED, CANCELLED, DISPUTED }
}
