package com.supplybase.partners.screening.dto;

import com.supplybase.partners.screening.ScreeningBooking;

import java.time.Instant;

public record BookingResponse(Long id, Long slotId, String status, String outcome, String outcomeReason,
                               Instant checkedInAt, Instant createdAt) {
    public static BookingResponse from(ScreeningBooking b) {
        return new BookingResponse(b.getId(), b.getSlotId(), b.getStatus().name(), b.getOutcome().name(),
                b.getOutcomeReason(), b.getCheckedInAt(), b.getCreatedAt());
    }
}
