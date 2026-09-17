package com.supplybase.partners.partner.dto;

import com.supplybase.partners.partner.WorkingHoursChoice;
import jakarta.validation.constraints.NotNull;

public record WorkingHoursRequest(@NotNull WorkingHoursChoice choice) {
}
