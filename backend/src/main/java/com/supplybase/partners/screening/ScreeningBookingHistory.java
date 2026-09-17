package com.supplybase.partners.screening;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "screening_booking_history")
@Getter
@Setter
@NoArgsConstructor
public class ScreeningBookingHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "booking_id", nullable = false)
    private Long bookingId;

    @Column(nullable = false, length = 32)
    private String event;

    @Column(name = "from_slot_id")
    private Long fromSlotId;

    @Column(name = "to_slot_id")
    private Long toSlotId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) createdAt = Instant.now();
    }

    public ScreeningBookingHistory(Long bookingId, String event, Long fromSlotId, Long toSlotId) {
        this.bookingId = bookingId;
        this.event = event;
        this.fromSlotId = fromSlotId;
        this.toSlotId = toSlotId;
    }
}
