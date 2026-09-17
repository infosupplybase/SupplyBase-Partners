package com.supplybase.partners.money;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EarningEntryRepository extends JpaRepository<EarningEntry, Long> {
    Optional<EarningEntry> findByServiceRequestId(Long serviceRequestId);

    List<EarningEntry> findAllByPartnerIdOrderByEarnedAtDesc(Long partnerId);
}
