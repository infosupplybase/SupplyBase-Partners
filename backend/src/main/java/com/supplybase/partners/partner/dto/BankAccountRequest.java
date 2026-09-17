package com.supplybase.partners.partner.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record BankAccountRequest(
        @NotBlank String accountHolderName,
        @NotBlank @Pattern(regexp = "\\d{9,18}", message = "Enter a valid account number (9-18 digits)") String accountNumber,
        @NotBlank @Pattern(regexp = "[A-Z]{4}0[A-Z0-9]{6}", message = "Enter a valid IFSC code") String ifsc,
        String bankName
) {
}
