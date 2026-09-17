package com.supplybase.partners.money;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PayoutEventRepository extends JpaRepository<PayoutEvent, Long> {
    Optional<PayoutEvent> findByProviderEventId(String providerEventId);
}
