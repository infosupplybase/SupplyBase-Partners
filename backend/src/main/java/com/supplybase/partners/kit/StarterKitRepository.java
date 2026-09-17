package com.supplybase.partners.kit;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface StarterKitRepository extends JpaRepository<StarterKit, Long> {
    Optional<StarterKit> findFirstByCategoryIdAndActiveTrue(Long categoryId);
}
