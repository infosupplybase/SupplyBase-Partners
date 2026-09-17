package com.supplybase.partners.screening.dto;

import com.supplybase.partners.screening.ScreeningSlot;

import java.time.Instant;

public record SlotResponse(Long id, Long categoryId, Long cityId, String mode, String venueOrLink,
                            String directionsText, Instant startsAt, Instant endsAt, int remainingCapacity) {
    public static SlotResponse from(ScreeningSlot s) {
        return new SlotResponse(s.getId(), s.getCategoryId(), s.getCityId(), s.getMode().name(), s.getVenueOrLink(),
                s.getDirectionsText(), s.getStartsAt(), s.getEndsAt(), s.remainingCapacity());
    }
}
