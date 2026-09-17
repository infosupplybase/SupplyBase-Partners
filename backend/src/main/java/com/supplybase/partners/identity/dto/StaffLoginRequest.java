package com.supplybase.partners.identity.dto;

import jakarta.validation.constraints.NotBlank;

public record StaffLoginRequest(@NotBlank String username, @NotBlank String password) {
}
