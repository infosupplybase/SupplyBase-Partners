package com.supplybase.partners.kit.dto;

import java.util.List;

public record QuoteResponse(boolean available, Long kitId, String name, String description,
                             long pricePaise, long feesPaise, long totalPaise, String termsText,
                             List<String> items) {
    public static QuoteResponse unavailable() {
        return new QuoteResponse(false, null, null, null, 0, 0, 0, null, List.of());
    }
}
