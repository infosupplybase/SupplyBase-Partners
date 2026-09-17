package com.supplybase.partners.kit;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StarterKitItemRepository extends JpaRepository<StarterKitItem, Long> {
    List<StarterKitItem> findAllByKitId(Long kitId);
}
