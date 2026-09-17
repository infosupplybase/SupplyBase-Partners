package com.supplybase.partners.training;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AssessmentOptionRepository extends JpaRepository<AssessmentOption, Long> {
    List<AssessmentOption> findAllByQuestionId(Long questionId);
}
