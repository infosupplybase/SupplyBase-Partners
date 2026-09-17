package com.supplybase.partners.partner;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PartnerConsentRepository extends JpaRepository<PartnerConsent, Long> {
    List<PartnerConsent> findAllByPartnerId(Long partnerId);

    boolean existsByPartnerIdAndPolicyDocumentId(Long partnerId, Long policyDocumentId);
}
