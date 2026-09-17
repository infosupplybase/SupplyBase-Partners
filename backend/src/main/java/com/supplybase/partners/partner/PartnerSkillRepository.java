package com.supplybase.partners.partner;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PartnerSkillRepository extends JpaRepository<PartnerSkill, Long> {
    List<PartnerSkill> findAllByPartnerId(Long partnerId);

    boolean existsByPartnerIdAndServiceId(Long partnerId, Long serviceId);
}
