package com.supplybase.partners.partner;

import com.supplybase.partners.partner.dto.BankAccountRequest;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Bank details: the DTO's @Pattern constraints are syntactic validation only.
 * Passing them earns status FORMAT_VALID, never "verified" -- true bank-provider
 * verification is a separate, unimplemented provider integration (see docs).
 */
@Service
public class BankAccountService {

    private static final BCryptPasswordEncoder HASHER = new BCryptPasswordEncoder();

    private final BankAccountRepository bankAccountRepository;

    public BankAccountService(BankAccountRepository bankAccountRepository) {
        this.bankAccountRepository = bankAccountRepository;
    }

    @Transactional
    public BankAccount upsert(Long partnerId, BankAccountRequest request) {
        BankAccount account = bankAccountRepository.findByPartnerId(partnerId).orElseGet(BankAccount::new);
        account.setPartnerId(partnerId);
        account.setAccountHolderName(request.accountHolderName().trim());
        account.setAccountNumberMasked(mask(request.accountNumber()));
        account.setAccountNumberHash(HASHER.encode(request.accountNumber()));
        account.setIfsc(request.ifsc().toUpperCase());
        account.setBankName(request.bankName());
        account.setVerificationStatus(BankAccount.VerificationStatus.FORMAT_VALID);
        return bankAccountRepository.save(account);
    }

    private String mask(String accountNumber) {
        int visible = Math.min(4, accountNumber.length());
        String tail = accountNumber.substring(accountNumber.length() - visible);
        return "X".repeat(Math.max(0, accountNumber.length() - visible)) + tail;
    }
}
