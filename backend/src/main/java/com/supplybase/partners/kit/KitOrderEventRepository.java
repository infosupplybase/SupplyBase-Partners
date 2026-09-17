package com.supplybase.partners.kit;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface KitOrderEventRepository extends JpaRepository<KitOrderEvent, Long> {
    List<KitOrderEvent> findAllByKitOrderIdOrderByCreatedAtAsc(Long kitOrderId);

    Optional<KitOrderEvent> findByProviderEventId(String providerEventId);
}
