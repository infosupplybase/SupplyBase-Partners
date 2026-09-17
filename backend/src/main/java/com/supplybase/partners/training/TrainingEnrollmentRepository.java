package com.supplybase.partners.training;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TrainingEnrollmentRepository extends JpaRepository<TrainingEnrollment, Long> {
    Optional<TrainingEnrollment> findByPartnerIdAndCourseId(Long partnerId, Long courseId);

    List<TrainingEnrollment> findAllByPartnerId(Long partnerId);
}
