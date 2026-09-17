package com.supplybase.partners.training;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AssessmentQuestionRepository extends JpaRepository<AssessmentQuestion, Long> {
    List<AssessmentQuestion> findAllByAssessmentIdOrderByDisplayOrder(Long assessmentId);
}
