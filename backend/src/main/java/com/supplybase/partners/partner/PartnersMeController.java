package com.supplybase.partners.partner;

import com.supplybase.partners.identity.CurrentUser;
import com.supplybase.partners.partner.dto.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

/**
 * Everything here derives the partner from the authenticated session
 * (CurrentUser), never from a client-supplied id -- a partner can only ever
 * read or write their own record.
 */
@RestController
@RequestMapping("/api/v1/partners/me")
@PreAuthorize("hasRole('PARTNER')")
public class PartnersMeController {

    private final PartnerService partnerService;
    private final CurrentUser currentUser;
    private final PartnerCoverageRepository coverageRepository;
    private final BankAccountService bankAccountService;
    private final BankAccountRepository bankAccountRepository;

    public PartnersMeController(PartnerService partnerService, CurrentUser currentUser,
                                 PartnerCoverageRepository coverageRepository, BankAccountService bankAccountService,
                                 BankAccountRepository bankAccountRepository) {
        this.partnerService = partnerService;
        this.currentUser = currentUser;
        this.coverageRepository = coverageRepository;
        this.bankAccountService = bankAccountService;
        this.bankAccountRepository = bankAccountRepository;
    }

    @GetMapping
    public PartnerMeResponse me() {
        return PartnerMeResponse.from(partnerService.getByUserId(currentUser.userId()));
    }

    @PatchMapping("/basic-details")
    public PartnerMeResponse basicDetails(@RequestBody BasicDetailsRequest body) {
        return PartnerMeResponse.from(partnerService.updateBasicDetails(currentUser.userId(), body.name(), body.preferredLanguage()));
    }

    @PutMapping("/category")
    public PartnerMeResponse category(@Valid @RequestBody SelectCategoryRequest body) {
        return PartnerMeResponse.from(partnerService.selectCategory(currentUser.userId(), body.categoryId()));
    }

    @PutMapping("/city")
    public PartnerMeResponse city(@Valid @RequestBody SelectCityRequest body) {
        return PartnerMeResponse.from(partnerService.selectCity(currentUser.userId(), body.cityId()));
    }

    @PostMapping("/consents")
    public PartnerMeResponse consents(@RequestBody ConsentRequest body, HttpServletRequest request) {
        return PartnerMeResponse.from(partnerService.acceptConsents(currentUser.userId(), body.marketingOptIn(), request.getRemoteAddr()));
    }

    @PostMapping("/earning-potential/ack")
    public PartnerMeResponse earningPotentialAck() {
        return PartnerMeResponse.from(partnerService.acknowledgeEarningPotential(currentUser.userId()));
    }

    @PutMapping("/working-hours")
    public PartnerMeResponse workingHours(@Valid @RequestBody WorkingHoursRequest body) {
        return PartnerMeResponse.from(partnerService.selectWorkingHours(currentUser.userId(), body.choice()));
    }

    @PostMapping("/permissions/ack")
    public PartnerMeResponse permissionsAck() {
        return PartnerMeResponse.from(partnerService.acknowledgePermissions(currentUser.userId()));
    }

    @PutMapping("/profile")
    @Transactional
    public PartnerMeResponse updateProfile(@Valid @RequestBody ProfileUpdateRequest body) {
        Partner partner = partnerService.getByUserId(currentUser.userId());
        if (body.experienceYears() != null) partner.setExperienceYears(body.experienceYears());
        if (body.bio() != null) partner.setBio(body.bio());
        if (body.languages() != null) {
            partner.getLanguages().clear();
            partner.getLanguages().addAll(body.languages());
        }
        return PartnerMeResponse.from(partner);
    }

    @PostMapping("/profile/complete")
    public PartnerMeResponse completeProfile() {
        return PartnerMeResponse.from(partnerService.completeProfileStep(currentUser.userId()));
    }

    @GetMapping("/coverage")
    public List<Long> coverage() {
        Partner partner = partnerService.getByUserId(currentUser.userId());
        return coverageRepository.findAllByPartnerId(partner.getId()).stream().map(PartnerCoverage::getAreaId).toList();
    }

    @PutMapping("/coverage")
    @Transactional
    public List<Long> updateCoverage(@RequestBody CoverageRequest body) {
        Partner partner = partnerService.getByUserId(currentUser.userId());
        List<PartnerCoverage> existing = coverageRepository.findAllByPartnerId(partner.getId());
        Set<Long> requested = body.areaIds() == null ? Set.of() : body.areaIds();
        existing.stream().filter(c -> !requested.contains(c.getAreaId())).forEach(coverageRepository::delete);
        Set<Long> already = existing.stream().map(PartnerCoverage::getAreaId).collect(java.util.stream.Collectors.toSet());
        requested.stream().filter(id -> !already.contains(id)).forEach(areaId -> {
            PartnerCoverage c = new PartnerCoverage();
            c.setPartnerId(partner.getId());
            c.setAreaId(areaId);
            coverageRepository.save(c);
        });
        return coverageRepository.findAllByPartnerId(partner.getId()).stream().map(PartnerCoverage::getAreaId).toList();
    }

    @GetMapping("/bank-account")
    public BankAccountResponse bankAccount() {
        Partner partner = partnerService.getByUserId(currentUser.userId());
        return bankAccountRepository.findByPartnerId(partner.getId())
                .map(a -> new BankAccountResponse(a.getId(), a.getAccountHolderName(), a.getAccountNumberMasked(),
                        a.getIfsc(), a.getBankName(), a.getVerificationStatus().name()))
                .orElse(null);
    }

    @PutMapping("/bank-account")
    public BankAccountResponse updateBankAccount(@Valid @RequestBody BankAccountRequest body) {
        Partner partner = partnerService.getByUserId(currentUser.userId());
        var account = bankAccountService.upsert(partner.getId(), body);
        return new BankAccountResponse(account.getId(), account.getAccountHolderName(), account.getAccountNumberMasked(),
                account.getIfsc(), account.getBankName(), account.getVerificationStatus().name());
    }
}
