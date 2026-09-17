package com.supplybase.partners.verification.dto;

import com.supplybase.partners.verification.VerificationRequest;

import java.time.Instant;

public record AdminVerificationItemResponse(Long partnerId, String provider, String status,
                                             String reason, Instant submittedAt) {
    public static AdminVerificationItemResponse from(VerificationRequest vr) {
        return new AdminVerificationItemResponse(vr.getPartnerId(), vr.getProvider().name(), vr.getStatus().name(),
                vr.getReason(), vr.getSubmittedAt());
    }
}
