package com.supplybase.partners.partner.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

import java.util.Set;

public record ProfileUpdateRequest(
        @Min(0) @Max(60) Integer experienceYears,
        @Size(max = 1000) String bio,
        Set<String> languages
) {
}
