package com.supplybase.partners.partner;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PartnerAvailabilityWindowRepository extends JpaRepository<PartnerAvailabilityWindow, Long> {
    List<PartnerAvailabilityWindow> findAllByPartnerId(Long partnerId);

    void deleteAllByPartnerId(Long partnerId);
}
