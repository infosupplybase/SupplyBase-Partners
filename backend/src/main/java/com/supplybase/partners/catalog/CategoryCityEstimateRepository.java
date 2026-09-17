package com.supplybase.partners.catalog;

import com.supplybase.partners.partner.WorkingHoursChoice;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CategoryCityEstimateRepository extends JpaRepository<CategoryCityEstimate, Long> {
    Optional<CategoryCityEstimate> findByCategoryIdAndCityIdAndHoursChoiceAndActiveTrue(
            Long categoryId, Long cityId, WorkingHoursChoice hoursChoice);
}
