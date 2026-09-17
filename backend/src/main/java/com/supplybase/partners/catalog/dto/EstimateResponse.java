package com.supplybase.partners.catalog.dto;

/** available=false means no configured estimate exists for this combination -- shown as an honest unavailable state, never a fabricated number. */
public record EstimateResponse(boolean available, boolean demo, Long estimatedMonthlyPaise, String assumptionsText) {
    public static EstimateResponse unavailable() {
        return new EstimateResponse(false, true, null, null);
    }
}
