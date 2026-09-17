package com.supplybase.partners.catalog;

import com.supplybase.partners.partner.WorkingHoursChoice;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "category_city_estimate")
@Getter
@Setter
@NoArgsConstructor
public class CategoryCityEstimate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "category_id", nullable = false)
    private Long categoryId;

    @Column(name = "city_id", nullable = false)
    private Long cityId;

    @Enumerated(EnumType.STRING)
    @Column(name = "hours_choice", nullable = false, length = 8)
    private WorkingHoursChoice hoursChoice;

    @Column(name = "estimated_monthly_paise", nullable = false)
    private long estimatedMonthlyPaise;

    @Column(name = "assumptions_text", nullable = false, length = 500)
    private String assumptionsText;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) createdAt = Instant.now();
    }
}
