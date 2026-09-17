package com.supplybase.partners.screening;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "screening_slot")
@Getter
@Setter
@NoArgsConstructor
public class ScreeningSlot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "category_id", nullable = false)
    private Long categoryId;

    @Column(name = "city_id", nullable = false)
    private Long cityId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private Mode mode;

    @Column(name = "venue_or_link", nullable = false, length = 500)
    private String venueOrLink;

    @Column(name = "directions_text", length = 500)
    private String directionsText;

    @Column(name = "starts_at", nullable = false)
    private Instant startsAt;

    @Column(name = "ends_at", nullable = false)
    private Instant endsAt;

    @Column(name = "check_in_opens_at", nullable = false)
    private Instant checkInOpensAt;

    @Column(name = "check_in_closes_at", nullable = false)
    private Instant checkInClosesAt;

    @Column(nullable = false)
    private int capacity;

    @Column(name = "booked_count", nullable = false)
    private int bookedCount;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Version
    private Long version;

    @PrePersist
    void prePersist() {
        if (createdAt == null) createdAt = Instant.now();
    }

    public int remainingCapacity() {
        return capacity - bookedCount;
    }

    public enum Mode { IN_PERSON, VIRTUAL }
}
