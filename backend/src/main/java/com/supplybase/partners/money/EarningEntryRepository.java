package com.supplybase.partners.money;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface EarningEntryRepository extends JpaRepository<EarningEntry, Long> {
    Optional<EarningEntry> findByServiceRequestId(Long serviceRequestId);

    List<EarningEntry> findAllByPartnerIdAndEarnedAtBetweenOrderByEarnedAtDesc(Long partnerId, Instant from, Instant to);

    List<EarningEntry> findAllByPartnerIdOrderByEarnedAtDesc(Long partnerId);
}
