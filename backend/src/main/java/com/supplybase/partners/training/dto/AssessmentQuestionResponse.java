package com.supplybase.partners.training.dto;

import java.util.List;

/** Options are shown without revealing which one is correct -- grading happens server-side. */
public record AssessmentQuestionResponse(Long id, String questionText, List<OptionResponse> options) {
    public record OptionResponse(Long id, String text) {}
}
