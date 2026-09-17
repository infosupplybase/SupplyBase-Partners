package com.supplybase.partners.partner;

import com.supplybase.partners.catalog.Category;
import com.supplybase.partners.catalog.CategoryRepository;
import com.supplybase.partners.catalog.City;
import com.supplybase.partners.catalog.CityRepository;
import com.supplybase.partners.catalog.PolicyDocument;
import com.supplybase.partners.catalog.PolicyDocumentRepository;
import com.supplybase.partners.common.domain.BadRequestException;
import com.supplybase.partners.common.domain.ForbiddenException;
import com.supplybase.partners.common.domain.NotFoundException;
import com.supplybase.partners.identity.AppUser;
import com.supplybase.partners.identity.AppUserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

/**
 * Onboarding/profile logic. Early free-form steps (basic details through
 * permissions) advance onboardingStage imperatively and only when the
 * partner is exactly at the stage that step completes -- so calling a step
 * endpoint out of order never corrupts progress, and editing already-passed
 * data later (e.g. from profile settings) never regresses the stage. Later
 * stages (verification onward) are advanced by their own domain services via
 * {@link #advanceStageIfCurrent}, since completion there is judged by other
 * entities' server-side status, not raw client input.
 */
@Service
public class PartnerService {

    private final PartnerRepository partnerRepository;
    private final PartnerStatusHistoryRepository statusHistoryRepository;
    private final AppUserRepository appUserRepository;
    private final CategoryRepository categoryRepository;
    private final CityRepository cityRepository;
    private final PolicyDocumentRepository policyDocumentRepository;
    private final PartnerConsentRepository partnerConsentRepository;

    public PartnerService(PartnerRepository partnerRepository, PartnerStatusHistoryRepository statusHistoryRepository,
                           AppUserRepository appUserRepository, CategoryRepository categoryRepository,
                           CityRepository cityRepository, PolicyDocumentRepository policyDocumentRepository,
                           PartnerConsentRepository partnerConsentRepository) {
        this.partnerRepository = partnerRepository;
        this.statusHistoryRepository = statusHistoryRepository;
        this.appUserRepository = appUserRepository;
        this.categoryRepository = categoryRepository;
        this.cityRepository = cityRepository;
        this.policyDocumentRepository = policyDocumentRepository;
        this.partnerConsentRepository = partnerConsentRepository;
    }

    @Transactional
    public Partner ensurePartnerExists(Long userId) {
        return partnerRepository.findByUserId(userId).orElseGet(() -> {
            Partner partner = new Partner();
            partner.setUserId(userId);
            partner.setOnboardingStage(OnboardingStage.BASIC_DETAILS);
            partner.setActivationStatus(Partner.ActivationStatus.NOT_ACTIVE);
            Partner saved = partnerRepository.save(partner);
            statusHistoryRepository.save(new PartnerStatusHistory(saved.getId(), null, "NOT_ACTIVE", "Application created", userId));
            return saved;
        });
    }

    public Partner getByUserId(Long userId) {
        return ensurePartnerExists(userId);
    }

    public Partner getByIdOrThrow(Long partnerId) {
        return partnerRepository.findById(partnerId).orElseThrow(() -> new NotFoundException("Partner not found."));
    }

    /** Throws if the caller is not the owner of this partner record -- ownership is always session-derived. */
    public Partner requireOwnedByUser(Long partnerId, Long userId) {
        Partner partner = getByIdOrThrow(partnerId);
        if (!partner.getUserId().equals(userId)) {
            throw new ForbiddenException("You do not own this partner record.");
        }
        return partner;
    }

    @Transactional
    public Partner updateBasicDetails(Long userId, String name, String preferredLanguage) {
        Partner partner = ensurePartnerExists(userId);
        AppUser user = appUserRepository.findById(userId).orElseThrow();
        if (name != null && !name.isBlank()) {
            user.setName(name.trim());
        }
        if (preferredLanguage != null && !preferredLanguage.isBlank()) {
            user.setPreferredLanguage(preferredLanguage.trim());
        }
        appUserRepository.save(user);
        advanceIfAt(partner, OnboardingStage.BASIC_DETAILS, OnboardingStage.WORK_CATEGORY);
        return partnerRepository.save(partner);
    }

    @Transactional
    public Partner selectCategory(Long userId, Long categoryId) {
        Partner partner = ensurePartnerExists(userId);
        Category category = categoryRepository.findById(categoryId)
                .filter(Category::isActive)
                .orElseThrow(() -> new NotFoundException("Category not found or inactive."));
        partner.setPrimaryCategoryId(category.getId());
        advanceIfAt(partner, OnboardingStage.WORK_CATEGORY, OnboardingStage.CITY);
        return partnerRepository.save(partner);
    }

    @Transactional
    public Partner selectCity(Long userId, Long cityId) {
        Partner partner = ensurePartnerExists(userId);
        if (partner.getPrimaryCategoryId() == null) {
            throw new BadRequestException("Select a work category before choosing a city.");
        }
        City city = cityRepository.findById(cityId)
                .filter(City::isActive)
                .orElseThrow(() -> new NotFoundException("City not found or not yet serviced."));
        partner.setResidenceCityId(city.getId());
        advanceIfAt(partner, OnboardingStage.CITY, OnboardingStage.TERMS_PRIVACY);
        return partnerRepository.save(partner);
    }

    @Transactional
    public Partner acceptConsents(Long userId, boolean marketingOptIn, String ipAddress) {
        Partner partner = ensurePartnerExists(userId);
        PolicyDocument terms = policyDocumentRepository.findByTypeAndCurrentTrue(PolicyDocument.PolicyType.TERMS)
                .orElseThrow(() -> new BadRequestException("No current Terms of Service is published."));
        PolicyDocument privacy = policyDocumentRepository.findByTypeAndCurrentTrue(PolicyDocument.PolicyType.PRIVACY)
                .orElseThrow(() -> new BadRequestException("No current Privacy Policy is published."));
        recordConsentIfMissing(partner.getId(), terms.getId(), ipAddress);
        recordConsentIfMissing(partner.getId(), privacy.getId(), ipAddress);
        if (marketingOptIn) {
            policyDocumentRepository.findByTypeAndCurrentTrue(PolicyDocument.PolicyType.MARKETING_CONSENT)
                    .ifPresent(marketing -> recordConsentIfMissing(partner.getId(), marketing.getId(), ipAddress));
        }
        advanceIfAt(partner, OnboardingStage.TERMS_PRIVACY, OnboardingStage.EARNING_POTENTIAL);
        return partnerRepository.save(partner);
    }

    private void recordConsentIfMissing(Long partnerId, Long policyDocumentId, String ipAddress) {
        if (partnerConsentRepository.existsByPartnerIdAndPolicyDocumentId(partnerId, policyDocumentId)) {
            return;
        }
        PartnerConsent consent = new PartnerConsent();
        consent.setPartnerId(partnerId);
        consent.setPolicyDocumentId(policyDocumentId);
        consent.setAccepted(true);
        consent.setIpAddress(ipAddress);
        partnerConsentRepository.save(consent);
    }

    @Transactional
    public Partner acknowledgeEarningPotential(Long userId) {
        Partner partner = ensurePartnerExists(userId);
        advanceIfAt(partner, OnboardingStage.EARNING_POTENTIAL, OnboardingStage.WORKING_HOURS);
        return partnerRepository.save(partner);
    }

    @Transactional
    public Partner selectWorkingHours(Long userId, WorkingHoursChoice choice) {
        Partner partner = ensurePartnerExists(userId);
        partner.setWorkingHoursChoice(choice);
        advanceIfAt(partner, OnboardingStage.WORKING_HOURS, OnboardingStage.PERMISSIONS);
        return partnerRepository.save(partner);
    }

    @Transactional
    public Partner acknowledgePermissions(Long userId) {
        Partner partner = ensurePartnerExists(userId);
        advanceIfAt(partner, OnboardingStage.PERMISSIONS, OnboardingStage.VERIFICATION);
        return partnerRepository.save(partner);
    }

    @Transactional
    public Partner completeProfileStep(Long userId) {
        Partner partner = ensurePartnerExists(userId);
        List<String> missing = new java.util.ArrayList<>();
        if (partner.getWorkingHoursChoice() == null) missing.add("workingHoursChoice");
        if (partner.getLanguages().isEmpty()) missing.add("languages");
        if (partner.getBio() == null || partner.getBio().isBlank()) missing.add("bio");
        if (!missing.isEmpty()) {
            throw new BadRequestException("Complete your profile before continuing: " + String.join(", ", missing));
        }
        advanceIfAt(partner, OnboardingStage.PROFILE, OnboardingStage.TRAINING);
        return partnerRepository.save(partner);
    }

    /** Called by verification/screening/kit/training services once their own gate is satisfied. */
    @Transactional
    public void advanceStageIfCurrent(Long partnerId, OnboardingStage from, OnboardingStage to) {
        Partner partner = getByIdOrThrow(partnerId);
        advanceIfAt(partner, from, to);
        partnerRepository.save(partner);
    }

    private void advanceIfAt(Partner partner, OnboardingStage from, OnboardingStage to) {
        if (partner.getOnboardingStage() == from) {
            partner.setOnboardingStage(to);
        }
    }

    @Transactional
    public void recordStatusChange(Long partnerId, String fromStatus, String toStatus, String reason, Long changedBy) {
        statusHistoryRepository.save(new PartnerStatusHistory(partnerId, fromStatus, toStatus, reason, changedBy));
    }

    public List<PartnerStatusHistory> statusHistory(Long partnerId) {
        return statusHistoryRepository.findAllByPartnerIdOrderByCreatedAtDesc(partnerId);
    }
}
