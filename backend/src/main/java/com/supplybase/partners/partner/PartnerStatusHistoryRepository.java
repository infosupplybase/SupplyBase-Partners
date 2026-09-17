package com.supplybase.partners.partner;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PartnerStatusHistoryRepository extends JpaRepository<PartnerStatusHistory, Long> {
    List<PartnerStatusHistory> findAllByPartnerIdOrderByCreatedAtDesc(Long partnerId);
}
