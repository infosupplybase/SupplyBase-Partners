package com.supplybase.partners.jobs.dto;

import java.time.Instant;

public record AssignedJobResponse(Long id, Long serviceId, Instant scheduledAt, String notes,
                                   long partnerEarningPaise, String status,
                                   String customerName, String customerPhone, String addressLine1, String addressLine2) {
}
