package com.supplybase.partners.verification.dto;

import com.supplybase.partners.verification.VerificationRequest;

import java.time.Instant;

public record VerificationStatusResponse(String provider, String status, String reason,
                                          Instant submittedAt, Instant decidedAt) {
    public static VerificationStatusResponse from(VerificationRequest vr) {
        return new VerificationStatusResponse(vr.getProvider().name(), vr.getStatus().name(), vr.getReason(),
                vr.getSubmittedAt(), vr.getDecidedAt());
    }
}
