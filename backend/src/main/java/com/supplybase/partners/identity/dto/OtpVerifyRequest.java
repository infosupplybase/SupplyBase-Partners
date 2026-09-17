package com.supplybase.partners.identity.dto;

import jakarta.validation.constraints.NotBlank;

public record OtpVerifyRequest(@NotBlank String phone, @NotBlank String code) {
}
