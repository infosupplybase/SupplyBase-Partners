package com.supplybase.partners.partner;

import com.supplybase.partners.catalog.PolicyDocument;
import com.supplybase.partners.catalog.PolicyDocumentRepository;
import com.supplybase.partners.common.domain.AuditService;
import com.supplybase.partners.common.domain.BadRequestException;
import com.supplybase.partners.common.domain.NotFoundException;
import com.supplybase.partners.screening.ScreeningBooking;
import com.supplybase.partners.screening.ScreeningBookingRepository;
import com.supplybase.partners.training.TrainingCourse;
import com.supplybase.partners.training.TrainingCourseRepository;
import com.supplybase.partners.training.TrainingEnrollment;
import com.supplybase.partners.training.TrainingEnrollmentRepository;
import com.supplybase.partners.verification.VerificationRequest;
import com.supplybase.partners.verification.VerificationRequestRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * The authoritative, defense-in-depth activation gate. onboardingStage
 * reaching ACTIVATION_REVIEW already implies each prior step's own service
 * advanced it correctly, but activation itself re-checks the underlying
 * facts directly (consents, verification, screening outcome, training)
 * rather than trusting the stage alone, since this is the point of no
 * return that unlocks real job assignment and payouts.
 */
@Service
public class ActivationService {

    private final PartnerRepository partnerRepository;
    private final PartnerStatusHistoryRepository statusHistoryRepository;
    private final PartnerConsentRepository partnerConsentRepository;
    private final PolicyDocumentRepository policyDocumentRepository;
    private final VerificationRequestRepository verificationRequestRepository;
    private final ScreeningBookingRepository screeningBookingRepository;
    private final TrainingEnrollmentRepository trainingEnrollmentRepository;
    private final TrainingCourseRepository trainingCourseRepository;
    private final AuditService auditService;

    public ActivationService(PartnerRepository partnerRepository, PartnerStatusHistoryRepository statusHistoryRepository,
                              PartnerConsentRepository partnerConsentRepository, PolicyDocumentRepository policyDocumentRepository,
                              VerificationRequestRepository verificationRequestRepository,
                              ScreeningBookingRepository screeningBookingRepository,
                              TrainingEnrollmentRepository trainingEnrollmentRepository,
                              TrainingCourseRepository trainingCourseRepository, AuditService auditService) {
        this.partnerRepository = partnerRepository;
        this.statusHistoryRepository = statusHistoryRepository;
        this.partnerConsentRepository = partnerConsentRepository;
        this.policyDocumentRepository = policyDocumentRepository;
        this.verificationRequestRepository = verificationRequestRepository;
        this.screeningBookingRepository = screeningBookingRepository;
        this.trainingEnrollmentRepository = trainingEnrollmentRepository;
        this.trainingCourseRepository = trainingCourseRepository;
        this.auditService = auditService;
    }

    public record EligibilityResult(boolean eligible, List<String> unmetRequirements) {}

    public EligibilityResult checkEligibility(Long partnerId) {
        Partner partner = partnerRepository.findById(partnerId).orElseThrow(() -> new NotFoundException("Partner not found."));
        List<String> unmet = new ArrayList<>();

        List<Long> acceptedPolicyIds = partnerConsentRepository.findAllByPartnerId(partnerId).stream()
                .map(PartnerConsent::getPolicyDocumentId).toList();
        for (PolicyDocument.PolicyType type : List.of(PolicyDocument.PolicyType.TERMS, PolicyDocument.PolicyType.PRIVACY)) {
            boolean accepted = policyDocumentRepository.findByTypeAndCurrentTrue(type)
                    .map(pd -> acceptedPolicyIds.contains(pd.getId())).orElse(false);
            if (!accepted) unmet.add("Current " + type + " not accepted");
        }

        boolean verified = verificationRequestRepository.findByPartnerId(partnerId)
                .map(vr -> vr.getStatus() == VerificationRequest.Status.APPROVED).orElse(false);
        if (!verified) unmet.add("Identity verification not approved");

        boolean screeningPassed = screeningBookingRepository.findAllByPartnerIdOrderByCreatedAtDesc(partnerId).stream()
                .anyMatch(b -> b.getOutcome() == ScreeningBooking.Outcome.PASSED);
        if (!screeningPassed) unmet.add("Screening session not passed");

        boolean kitGateCleared = partner.getOnboardingStage() != OnboardingStage.STARTER_KIT
                && partner.getOnboardingStage() != OnboardingStage.SCREENING
                && partner.getOnboardingStage() != OnboardingStage.VERIFICATION;
        if (!kitGateCleared) unmet.add("Starter kit requirement not fulfilled or exempted");

        boolean profileComplete = partner.getWorkingHoursChoice() != null && !partner.getLanguages().isEmpty()
                && partner.getBio() != null && !partner.getBio().isBlank();
        if (!profileComplete) unmet.add("Required profile fields incomplete");

        List<TrainingCourse> required = trainingCourseRepository.findAllByRequiredTrueAndActiveTrue();
        boolean trainingComplete = required.stream().allMatch(c ->
                trainingEnrollmentRepository.findByPartnerIdAndCourseId(partnerId, c.getId())
                        .map(e -> e.getStatus() == TrainingEnrollment.Status.COMPLETED).orElse(false));
        if (!trainingComplete) unmet.add("Required training not completed");

        return new EligibilityResult(unmet.isEmpty(), unmet);
    }

    @Transactional
    public Partner activate(Long partnerId, Long actorUserId) {
        EligibilityResult result = checkEligibility(partnerId);
        if (!result.eligible()) {
            throw new BadRequestException("Partner is not eligible for activation: " + String.join("; ", result.unmetRequirements()));
        }
        Partner partner = partnerRepository.findById(partnerId).orElseThrow(() -> new NotFoundException("Partner not found."));
        partner.setActivationStatus(Partner.ActivationStatus.ACTIVE);
        partner.setOnboardingStage(OnboardingStage.COMPLETE);
        partner.setActivatedAt(Instant.now());
        partner.setSuspendedReason(null);
        partnerRepository.save(partner);

        statusHistoryRepository.save(new PartnerStatusHistory(partnerId, "NOT_ACTIVE", "ACTIVE", "All activation requirements met", actorUserId));
        auditService.record(actorUserId, "PARTNER_ACTIVATED", "Partner", partnerId, "All activation requirements met");
        return partner;
    }

    @Transactional
    public Partner suspend(Long partnerId, String reason, Long actorUserId) {
        Partner partner = partnerRepository.findById(partnerId).orElseThrow(() -> new NotFoundException("Partner not found."));
        if (partner.getActivationStatus() != Partner.ActivationStatus.ACTIVE) {
            throw new BadRequestException("Only an active partner can be suspended.");
        }
        partner.setActivationStatus(Partner.ActivationStatus.SUSPENDED);
        partner.setSuspendedReason(reason);
        partnerRepository.save(partner);
        statusHistoryRepository.save(new PartnerStatusHistory(partnerId, "ACTIVE", "SUSPENDED", reason, actorUserId));
        auditService.record(actorUserId, "PARTNER_SUSPENDED", "Partner", partnerId, reason);
        return partner;
    }

    @Transactional
    public Partner reactivate(Long partnerId, String reason, Long actorUserId) {
        Partner partner = partnerRepository.findById(partnerId).orElseThrow(() -> new NotFoundException("Partner not found."));
        if (partner.getActivationStatus() != Partner.ActivationStatus.SUSPENDED) {
            throw new BadRequestException("Only a suspended partner can be reactivated.");
        }
        partner.setActivationStatus(Partner.ActivationStatus.ACTIVE);
        partner.setSuspendedReason(null);
        partnerRepository.save(partner);
        statusHistoryRepository.save(new PartnerStatusHistory(partnerId, "SUSPENDED", "ACTIVE", reason, actorUserId));
        auditService.record(actorUserId, "PARTNER_REACTIVATED", "Partner", partnerId, reason);
        return partner;
    }
}
