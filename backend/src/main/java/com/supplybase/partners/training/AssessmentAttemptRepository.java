package com.supplybase.partners.training;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AssessmentAttemptRepository extends JpaRepository<AssessmentAttempt, Long> {
    List<AssessmentAttempt> findAllByAssessmentIdAndPartnerIdOrderByAttemptNumberDesc(Long assessmentId, Long partnerId);
}
