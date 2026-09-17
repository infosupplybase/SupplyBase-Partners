package com.supplybase.partners.training;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TrainingModuleRepository extends JpaRepository<TrainingModule, Long> {
    List<TrainingModule> findAllByCourseIdOrderByDisplayOrder(Long courseId);
}
