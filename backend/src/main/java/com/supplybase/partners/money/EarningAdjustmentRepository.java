package com.supplybase.partners.money;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EarningAdjustmentRepository extends JpaRepository<EarningAdjustment, Long> {
    List<EarningAdjustment> findAllByPartnerIdOrderByCreatedAtDesc(Long partnerId);
}
