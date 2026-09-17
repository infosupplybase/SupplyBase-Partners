package com.supplybase.partners.partner.dto;

import com.supplybase.partners.partner.OnboardingStage;
import com.supplybase.partners.partner.Partner;
import com.supplybase.partners.partner.WorkingHoursChoice;

import java.util.Set;

public record PartnerMeResponse(
        Long id,
        Long primaryCategoryId,
        Long residenceCityId,
        Integer experienceYears,
        String bio,
        String photoStorageKey,
        WorkingHoursChoice workingHoursChoice,
        OnboardingStage onboardingStage,
        String activationStatus,
        String suspendedReason,
        Set<String> languages
) {
    public static PartnerMeResponse from(Partner p) {
        return new PartnerMeResponse(
                p.getId(), p.getPrimaryCategoryId(), p.getResidenceCityId(), p.getExperienceYears(), p.getBio(),
                p.getPhotoStorageKey(), p.getWorkingHoursChoice(), p.getOnboardingStage(),
                p.getActivationStatus().name(), p.getSuspendedReason(), p.getLanguages());
    }
}
