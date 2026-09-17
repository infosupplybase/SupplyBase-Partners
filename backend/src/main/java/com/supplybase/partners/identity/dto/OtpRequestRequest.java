package com.supplybase.partners.identity.dto;

import jakarta.validation.constraints.NotBlank;

public record OtpRequestRequest(@NotBlank String phone) {
}
