package com.supplybase.partners.jobs.dto;

import java.time.Instant;

/** Deliberately omits customer name/phone and the exact address -- only shown once assigned. */
public record NearbyJobResponse(Long id, Long serviceId, Instant scheduledAt, String notes,
                                 long partnerEarningPaise, String areaName, String cityName) {
}
