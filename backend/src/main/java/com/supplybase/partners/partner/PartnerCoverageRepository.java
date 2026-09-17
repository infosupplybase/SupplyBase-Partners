package com.supplybase.partners.partner;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PartnerCoverageRepository extends JpaRepository<PartnerCoverage, Long> {
    List<PartnerCoverage> findAllByPartnerId(Long partnerId);

    List<PartnerCoverage> findAllByAreaId(Long areaId);

    boolean existsByPartnerIdAndAreaId(Long partnerId, Long areaId);
}
