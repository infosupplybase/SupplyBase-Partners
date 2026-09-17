package com.supplybase.partners.partner.dto;

public record BankAccountResponse(Long id, String accountHolderName, String accountNumberMasked,
                                   String ifsc, String bankName, String verificationStatus) {
}
