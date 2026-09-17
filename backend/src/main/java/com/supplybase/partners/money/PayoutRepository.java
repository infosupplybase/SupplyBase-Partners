package com.supplybase.partners.money;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PayoutRepository extends JpaRepository<Payout, Long> {
    List<Payout> findAllByPartnerIdOrderByRequestedAtDesc(Long partnerId);
}
