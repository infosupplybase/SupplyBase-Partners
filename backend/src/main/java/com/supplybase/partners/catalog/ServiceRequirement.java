package com.supplybase.partners.catalog;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "service_requirement")
@Getter
@Setter
@NoArgsConstructor
public class ServiceRequirement {

    @Id
    @Column(name = "service_id")
    private Long serviceId;

    @Column(name = "starter_kit_required", nullable = false)
    private boolean starterKitRequired = true;

    @Column(name = "required_course_id")
    private Long requiredCourseId;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    @PreUpdate
    void touch() {
        updatedAt = Instant.now();
    }
}
