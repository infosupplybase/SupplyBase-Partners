package com.supplybase.partners.training;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ModuleProgressRepository extends JpaRepository<ModuleProgress, Long> {
    Optional<ModuleProgress> findByEnrollmentIdAndModuleId(Long enrollmentId, Long moduleId);

    List<ModuleProgress> findAllByEnrollmentId(Long enrollmentId);
}
