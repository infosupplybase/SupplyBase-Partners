package com.supplybase.partners.catalog;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ServiceRepository extends JpaRepository<Service, Long> {
    List<Service> findAllByCategoryIdAndActiveTrueOrderByDisplayOrder(Long categoryId);

    Optional<Service> findBySlug(String slug);
}
