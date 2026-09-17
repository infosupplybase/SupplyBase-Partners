package com.supplybase.partners.kit;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface KitOrderRepository extends JpaRepository<KitOrder, Long> {
    List<KitOrder> findAllByPartnerIdOrderByCreatedAtDesc(Long partnerId);

    Optional<KitOrder> findByIdempotencyKey(String idempotencyKey);
}
